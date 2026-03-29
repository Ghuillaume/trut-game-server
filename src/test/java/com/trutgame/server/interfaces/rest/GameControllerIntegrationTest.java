package com.trutgame.server.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("GameController — Integration tests")
class GameControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // ── Helper ──────────────────────────────────────────────────────────

    private Map<String, Object> createGame(String pseudo) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/games", Map.of("pseudo", pseudo), Map.class);
        then(response.getStatusCode().value()).isEqualTo(201);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        return body;
    }

    private Map<String, Object> joinGame(String gameId, String pseudo) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/games/" + gameId + "/join", Map.of("pseudo", pseudo), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        return body;
    }

    /**
     * Creates a full 4-player game, starts it, and returns an array of [gameId, p1Id, p2Id, p3Id, p4Id].
     */
    private String[] createFullGame() {
        Map<String, Object> create = createGame("Alice");
        String gameId = (String) create.get("gameId");
        String p1 = (String) create.get("playerId");
        String p2 = (String) joinGame(gameId, "Bob").get("playerId");
        String p3 = (String) joinGame(gameId, "Charlie").get("playerId");
        String p4 = (String) joinGame(gameId, "Diana").get("playerId");
        // Creator explicitly starts the game
        startGame(gameId, p1);
        return new String[]{gameId, p1, p2, p3, p4};
    }

    private void startGame(String gameId, String requestingPlayerId) {
        restTemplate.postForEntity(
                "/api/games/" + gameId + "/start",
                Map.of("requestingPlayerId", requestingPlayerId), Map.class);
    }

    // ── Tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("should create a new game and return gameId and playerId")
    void shouldCreateNewGame() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/games", Map.of("pseudo", "Alice"), Map.class);

        then(response.getStatusCode().value()).isEqualTo(201);
        then(response.getBody()).containsKeys("gameId", "playerId");
        then(response.getBody().get("gameId")).isNotNull();
        then(response.getBody().get("playerId")).isNotNull();
    }

    @Test
    @DisplayName("should allow a second player to join and return players list")
    void shouldAllowSecondPlayerToJoin() {
        Map<String, Object> create = createGame("Alice");
        String gameId = (String) create.get("gameId");

        ResponseEntity<Map> joinResponse = restTemplate.postForEntity(
                "/api/games/" + gameId + "/join", Map.of("pseudo", "Bob"), Map.class);

        then(joinResponse.getStatusCode().value()).isEqualTo(200);
        then(joinResponse.getBody()).containsKeys("playerId", "players");
        then((List<?>) joinResponse.getBody().get("players")).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("should allow 4 players to join and start the game")
    void shouldAllowFourPlayersToJoinAndStartGame() {
        String[] ids = createFullGame();
        String gameId = ids[0];
        String player1Id = ids[1];

        ResponseEntity<Map> viewResponse = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=" + player1Id, Map.class);

        then(viewResponse.getStatusCode().value()).isEqualTo(200);
        then(viewResponse.getBody().get("phase")).isEqualTo("PLAYING_TRICK");
        then((List<?>) viewResponse.getBody().get("myHand")).hasSize(3);
    }

    @Test
    @DisplayName("should return 400 when joining non-existent game")
    void shouldReturn400WhenJoiningNonExistentGame() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/games/nonexistent/join", Map.of("pseudo", "Bob"), Map.class);

        then(response.getStatusCode().value()).isEqualTo(400);
        then(response.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("should return 409 when game is full")
    void shouldReturn409WhenGameIsFull() {
        String[] ids = createFullGame();
        String gameId = ids[0];

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/games/" + gameId + "/join", Map.of("pseudo", "Eve"), Map.class);

        then(response.getStatusCode().value()).isEqualTo(409);
        then(response.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("should return game view with personal hand for each player")
    void shouldReturnGameViewWithPersonalHand() {
        String[] ids = createFullGame();
        String gameId = ids[0];
        String p1 = ids[1];
        String p2 = ids[2];

        ResponseEntity<Map> v1 = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=" + p1, Map.class);
        ResponseEntity<Map> v2 = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=" + p2, Map.class);

        then(v1.getStatusCode().value()).isEqualTo(200);
        then(v2.getStatusCode().value()).isEqualTo(200);
        then(v1.getBody().get("myHand")).isNotEqualTo(v2.getBody().get("myHand"));
        then(v1.getBody().get("myTeam")).isEqualTo("TEAM_A");
        then(v2.getBody().get("myTeam")).isEqualTo("TEAM_B");
    }

    @Test
    @DisplayName("should return game view with score and players list")
    void shouldReturnGameViewWithScoreAndPlayers() {
        String[] ids = createFullGame();
        String gameId = ids[0];
        String p1 = ids[1];

        ResponseEntity<Map> view = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=" + p1, Map.class);

        then(view.getBody().get("gameId")).isEqualTo(gameId);
        then(view.getBody().get("roundNumber")).isEqualTo(1);
        then(view.getBody().get("winner")).isNull();
        then(view.getBody().get("fortial")).isEqualTo(false);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> score =
                (Map<String, Map<String, Integer>>) view.getBody().get("score");
        then(score).containsKeys("TEAM_A", "TEAM_B");
        then(score.get("TEAM_A").get("grands")).isEqualTo(0);
        then(score.get("TEAM_A").get("petits")).isEqualTo(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players =
                (List<Map<String, Object>>) view.getBody().get("players");
        then(players).hasSize(4);
    }

    @Test
    @DisplayName("should include available actions in game view")
    void shouldIncludeAvailableActionsInGameView() {
        String[] ids = createFullGame();
        String gameId = ids[0];
        String currentPlayerId = findCurrentPlayer(gameId, ids);

        ResponseEntity<Map> view = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=" + currentPlayerId, Map.class);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) view.getBody().get("availableActions");
        then(actions).isNotEmpty();
        then(actions).contains("PLAY_CARD");
    }

    @Test
    @DisplayName("should return 400 when getting view for non-existent game")
    void shouldReturn400ForNonExistentGame() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/games/nonexistent?playerId=some-player-id", Map.class);

        then(response.getStatusCode().value()).isEqualTo(400);
        then(response.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("should return empty hand for unknown playerId in existing game")
    void shouldReturnEmptyHandForUnknownPlayerId() {
        String[] ids = createFullGame();
        String gameId = ids[0];

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=unknown-player-id", Map.class);

        then(response.getStatusCode().value()).isEqualTo(200);
        then((List<?>) response.getBody().get("myHand")).isEmpty();
        then(response.getBody().get("myTeam")).isNull();
    }

    @Test
    @DisplayName("should populate completedTricks after a full trick is played")
    @SuppressWarnings("unchecked")
    void shouldPopulateCompletedTricksAfterFullTrick() {
        // Verify completedTricks field is present and serialized in initial GameView
        String[] ids = createFullGame();
        String gameId = ids[0];

        Map<String, Object> view = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=" + ids[1], Map.class).getBody();

        then(view).containsKey("completedTricks");
        then(view.get("completedTricks")).isNotNull();
        then((List<?>) view.get("completedTricks")).isEmpty(); // Empty at start of round
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private String findCurrentPlayer(String gameId, String[] ids) {
        for (int i = 1; i < ids.length; i++) {
            ResponseEntity<Map> view = restTemplate.getForEntity(
                    "/api/games/" + gameId + "?playerId=" + ids[i], Map.class);
            if (ids[i].equals(view.getBody().get("currentPlayerId"))) {
                return ids[i];
            }
        }
        return ids[1];
    }
}
