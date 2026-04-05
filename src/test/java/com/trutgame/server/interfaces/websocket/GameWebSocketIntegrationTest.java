package com.trutgame.server.interfaces.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("GameWebSocket — Integration tests")
class GameWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        var transport = new WebSocketTransport(new StandardWebSocketClient());
        var sockJsClient = new SockJsClient(List.of(transport));
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new CompositeMessageConverter(
                List.of(new MappingJackson2MessageConverter(), new StringMessageConverter())));
    }

    @AfterEach
    void cleanup() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    // ── Helper methods ──────────────────────────────────────────────────

    private StompSession connectStomp() throws Exception {
        String url = "ws://localhost:" + port + "/ws";
        return stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createGame(String pseudo) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/games", Map.of("pseudo", pseudo), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> joinGame(String gameId, String pseudo) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/games/" + gameId + "/join", Map.of("pseudo", pseudo), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGameView(String gameId, String playerId) {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/games/" + gameId + "?playerId=" + playerId, Map.class);
        return response.getBody();
    }

    /**
     * Creates a full 4-player game, starts it, and returns [gameId, p1Id, p2Id, p3Id, p4Id].
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

    /**
     * Finds the current player and returns [playerId, firstCardId].
     */
    @SuppressWarnings("unchecked")
    private String[] findCurrentPlayerAndCard(String gameId, String[] playerIds) {
        for (String pid : playerIds) {
            Map<String, Object> view = getGameView(gameId, pid);
            if (pid.equals(view.get("currentPlayerId"))) {
                List<String> hand = (List<String>) view.get("myHand");
                return new String[]{pid, hand.get(0)};
            }
        }
        throw new IllegalStateException("No current player found");
    }

    // ── Tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("should connect to WebSocket STOMP endpoint successfully")
    void shouldConnectToStompEndpoint() throws Exception {
        StompSession session = connectStomp();

        then(session.isConnected()).isTrue();

        session.disconnect();
    }

    @Test
    @DisplayName("should receive updated GameView after playing a card")
    void shouldReceiveGameViewAfterPlayingCard() throws Exception {
        // Arrange: create a full game
        String[] ids = createFullGame();
        String gameId = ids[0];
        String[] playerIds = {ids[1], ids[2], ids[3], ids[4]};
        String[] currentInfo = findCurrentPlayerAndCard(gameId, playerIds);
        String currentPlayerId = currentInfo[0];
        String cardToPlay = currentInfo[1];

        // Connect and subscribe
        StompSession session = connectStomp();
        CompletableFuture<Map> receivedView = new CompletableFuture<>();

        session.subscribe(
                "/topic/games/" + gameId + "/player/" + currentPlayerId,
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void handleFrame(StompHeaders headers, Object payload) {
                        receivedView.complete((Map) payload);
                    }
                });

        // Small delay to let the subscription register
        Thread.sleep(500);

        // Act: send a PLAY_CARD action
        session.send(
                "/app/games/" + gameId + "/action",
                Map.of("playerId", currentPlayerId,
                       "type", "PLAY_CARD",
                       "cardId", cardToPlay));

        // Assert: should receive an updated game view
        Map<String, Object> view = receivedView.get(5, TimeUnit.SECONDS);
        then(view).containsKey("gameId");
        then(view.get("gameId")).isEqualTo(gameId);
        then(view).containsKey("phase");
        then(view).containsKey("currentTrick");

        session.disconnect();
    }

    @Test
    @DisplayName("should receive event after a card is played")
    void shouldReceiveEventAfterAction() throws Exception {
        // Arrange: create a full game
        String[] ids = createFullGame();
        String gameId = ids[0];
        String[] playerIds = {ids[1], ids[2], ids[3], ids[4]};
        String[] currentInfo = findCurrentPlayerAndCard(gameId, playerIds);
        String currentPlayerId = currentInfo[0];
        String cardToPlay = currentInfo[1];

        // Connect and subscribe to events
        StompSession session = connectStomp();
        CompletableFuture<String> receivedEvent = new CompletableFuture<>();

        session.subscribe(
                "/topic/games/" + gameId + "/events",
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        receivedEvent.complete((String) payload);
                    }
                });

        Thread.sleep(500);

        // Act: play a card
        session.send(
                "/app/games/" + gameId + "/action",
                Map.of("playerId", currentPlayerId,
                       "type", "PLAY_CARD",
                       "cardId", cardToPlay));

        // Assert: should receive an event message
        String event = receivedEvent.get(5, TimeUnit.SECONDS);
        then(event).isNotBlank();

        session.disconnect();
    }

    @Test
    @DisplayName("should receive error when playing invalid card")
    void shouldReceiveErrorWhenPlayingInvalidCard() throws Exception {
        // Arrange: create a full game
        String[] ids = createFullGame();
        String gameId = ids[0];
        String[] playerIds = {ids[1], ids[2], ids[3], ids[4]};
        String[] currentInfo = findCurrentPlayerAndCard(gameId, playerIds);
        String currentPlayerId = currentInfo[0];

        // Connect and subscribe to error topic
        StompSession session = connectStomp();
        CompletableFuture<Map> receivedError = new CompletableFuture<>();

        session.subscribe(
                "/topic/games/" + gameId + "/player/" + currentPlayerId + "/errors",
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void handleFrame(StompHeaders headers, Object payload) {
                        receivedError.complete((Map) payload);
                    }
                });

        Thread.sleep(500);

        // Act: send an action with a card the player doesn't have
        session.send(
                "/app/games/" + gameId + "/action",
                Map.of("playerId", currentPlayerId,
                       "type", "PLAY_CARD",
                       "cardId", "INVALID_CARD"));

        // Assert: should receive an error
        Map<String, Object> error = receivedError.get(5, TimeUnit.SECONDS);
        then(error).containsKey("error");

        session.disconnect();
    }

    @Test
    @DisplayName("should receive game views for all subscribed players after action")
    void shouldReceiveViewsForAllSubscribedPlayers() throws Exception {
        // Arrange
        String[] ids = createFullGame();
        String gameId = ids[0];
        String[] playerIds = {ids[1], ids[2], ids[3], ids[4]};
        String[] currentInfo = findCurrentPlayerAndCard(gameId, playerIds);
        String currentPlayerId = currentInfo[0];
        String cardToPlay = currentInfo[1];

        // Pick another player to also subscribe
        String otherPlayerId = playerIds[0].equals(currentPlayerId) ? playerIds[1] : playerIds[0];

        StompSession session = connectStomp();
        CompletableFuture<Map> currentPlayerView = new CompletableFuture<>();
        CompletableFuture<Map> otherPlayerView = new CompletableFuture<>();

        session.subscribe(
                "/topic/games/" + gameId + "/player/" + currentPlayerId,
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void handleFrame(StompHeaders headers, Object payload) {
                        currentPlayerView.complete((Map) payload);
                    }
                });

        session.subscribe(
                "/topic/games/" + gameId + "/player/" + otherPlayerId,
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void handleFrame(StompHeaders headers, Object payload) {
                        otherPlayerView.complete((Map) payload);
                    }
                });

        Thread.sleep(500);

        // Act
        session.send(
                "/app/games/" + gameId + "/action",
                Map.of("playerId", currentPlayerId,
                       "type", "PLAY_CARD",
                       "cardId", cardToPlay));

        // Assert: both players should receive their views
        Map<String, Object> v1 = currentPlayerView.get(5, TimeUnit.SECONDS);
        Map<String, Object> v2 = otherPlayerView.get(5, TimeUnit.SECONDS);

        then(v1.get("gameId")).isEqualTo(gameId);
        then(v2.get("gameId")).isEqualTo(gameId);
        // Each player sees their own hand, so they should differ
        then(v1.get("myHand")).isNotEqualTo(v2.get("myHand"));

        session.disconnect();
    }

    @Test
    @DisplayName("should populate completedTricks after playing a full trick via WebSocket")
    @SuppressWarnings("unchecked")
    void shouldPopulateCompletedTricksAfterFullTrick() throws Exception {
        // Arrange
        String[] ids = createFullGame();
        String gameId = ids[0];
        String[] playerIds = {ids[1], ids[2], ids[3], ids[4]};

        StompSession session = connectStomp();

        // Use a blocking queue to receive sequential views for the observer player
        java.util.concurrent.BlockingQueue<Map> viewQueue = new java.util.concurrent.LinkedBlockingQueue<>();
        session.subscribe(
                "/topic/games/" + gameId + "/player/" + ids[1],
                new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return Map.class; }
                    @Override public void handleFrame(StompHeaders h, Object p) { viewQueue.offer((Map) p); }
                });
        Thread.sleep(500);

        // Play 4 cards (1 full trick)
        for (int card = 0; card < 4; card++) {
            String[] info = findCurrentPlayerAndCard(gameId, playerIds);
            session.send(
                    "/app/games/" + gameId + "/action",
                    Map.of("playerId", info[0], "type", "PLAY_CARD", "cardId", info[1]));
            // Wait for the view update
            Map view = viewQueue.poll(5, TimeUnit.SECONDS);
            then(view).as("Should receive view after card %d", card).isNotNull();
        }

        // After 4 cards, first trick is complete. Get the latest view via REST.
        Map<String, Object> afterTrick = getGameView(gameId, ids[1]);
        List<Map<String, Object>> completed = (List<Map<String, Object>>) afterTrick.get("completedTricks");

        then(completed).as("completedTricks should have 1 entry after first trick").hasSize(1);
        then(completed.get(0)).containsKey("entries");
        then(completed.get(0)).containsKey("winnerTeam");
        List<?> entries = (List<?>) completed.get(0).get("entries");
        then(entries).hasSize(4);

        session.disconnect();
    }

    @Test
    @DisplayName("should handle START_NEW_ROUND after all tricks are played")
    @SuppressWarnings("unchecked")
    void shouldHandleStartNewRoundAfterEndOfRound() throws Exception {
        // Arrange
        String[] ids = createFullGame();
        String gameId = ids[0];
        String[] playerIds = {ids[1], ids[2], ids[3], ids[4]};

        StompSession session = connectStomp();
        java.util.concurrent.BlockingQueue<Map> viewQueue = new java.util.concurrent.LinkedBlockingQueue<>();
        java.util.concurrent.BlockingQueue<Map> errorQueue = new java.util.concurrent.LinkedBlockingQueue<>();

        session.subscribe("/topic/games/" + gameId + "/player/" + ids[1],
                new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return Map.class; }
                    @Override public void handleFrame(StompHeaders h, Object p) { viewQueue.offer((Map) p); }
                });
        session.subscribe("/topic/games/" + gameId + "/player/" + ids[1] + "/errors",
                new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return Map.class; }
                    @Override public void handleFrame(StompHeaders h, Object p) { errorQueue.offer((Map) p); }
                });
        Thread.sleep(500);

        // Play all 12 cards (3 tricks) — handle potential TRUT_CHALLENGE phases
        int maxMoves = 20;
        int moves = 0;
        while (moves < maxMoves) {
            Map<String, Object> currentView = getGameView(gameId, ids[1]);
            String phase = (String) currentView.get("phase");

            if ("END_OF_ROUND".equals(phase) || "GAME_OVER".equals(phase)) break;

            if ("TRUT_CHALLENGE".equals(phase)) {
                // Fold to skip the challenge
                String responder = (String) currentView.get("currentPlayerId");
                session.send("/app/games/" + gameId + "/action",
                        Map.of("playerId", responder, "type", "FOLD"));
            } else if ("PLAYING_TRICK".equals(phase)) {
                String[] info = findCurrentPlayerAndCard(gameId, playerIds);
                session.send("/app/games/" + gameId + "/action",
                        Map.of("playerId", info[0], "type", "PLAY_CARD", "cardId", info[1]));
            } else {
                break;
            }
            // Consume view update
            viewQueue.poll(3, TimeUnit.SECONDS);
            moves++;
        }

        // After all tricks, verify we reached END_OF_ROUND
        Map<String, Object> endView = getGameView(gameId, ids[1]);
        String phase = (String) endView.get("phase");
        // Could be END_OF_ROUND or GAME_OVER
        then(phase).isIn("END_OF_ROUND", "GAME_OVER");

        if ("END_OF_ROUND".equals(phase)) {
            // Verify completedTricks has 2 or 3 entries (round may end early when decided after 2 tricks)
            List<Map<String, Object>> completed = (List<Map<String, Object>>) endView.get("completedTricks");
            then(completed).as("completedTricks should have 2 or 3 entries at end of round").hasSizeBetween(2, 3);

            // Now send START_NEW_ROUND
            session.send("/app/games/" + gameId + "/action",
                    Map.of("playerId", ids[1], "type", "START_NEW_ROUND"));

            // Should receive a new game view, not an error
            Map view = viewQueue.poll(5, TimeUnit.SECONDS);
            Map error = errorQueue.poll(1, TimeUnit.SECONDS);

            then(error).as("Should NOT receive an error for START_NEW_ROUND").isNull();
            then(view).as("Should receive a new GameView after START_NEW_ROUND").isNotNull();
            then(view.get("phase")).isIn("PLAYING_TRICK", "FORTIAL_DECISION");
        }

        session.disconnect();
    }
}
