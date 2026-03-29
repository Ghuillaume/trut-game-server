package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.dto.JoinGameCommand;
import com.trutgame.server.application.dto.JoinGameResult;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.TokenCount;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.TrutGameEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JoinGameService — unit tests")
class JoinGameServiceTest {

    private static final String GAME_ID = "game-1";
    private static final String PSEUDO = "Bob";

    @Mock
    private GameSessionRepository repository;

    @Mock
    private GameViewPublisher publisher;

    @Mock
    private TrutGameEngine engine;

    @Mock
    private GameViewBuilder viewBuilder;

    @InjectMocks
    private JoinGameService service;

    @Test
    @DisplayName("should join an existing game and return player info")
    void shouldJoinExistingGame() {
        // Given
        var host = new Player(new PlayerId("host-id"), "Alice", Team.TEAM_A, 0, false);
        GameState state = createWaitingState(List.of(host));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class)))
            .willReturn(dummyView());

        // When
        JoinGameResult result = service.joinGame(new JoinGameCommand(GAME_ID, PSEUDO));

        // Then
        then(result.playerId()).isNotBlank();
        then(result.players()).hasSize(2);
        then(result.players().get(1).pseudo()).isEqualTo(PSEUDO);
    }

    @Test
    @DisplayName("should throw when game is not found")
    void shouldThrowWhenGameNotFound() {
        // Given
        given(repository.findById(GAME_ID)).willReturn(Optional.empty());

        // When / Then
        thenThrownBy(() -> service.joinGame(new JoinGameCommand(GAME_ID, PSEUDO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Game not found");
    }

    @Test
    @DisplayName("should throw when game has already started")
    void shouldThrowWhenGameAlreadyStarted() {
        // Given
        var host = new Player(new PlayerId("host-id"), "Alice", Team.TEAM_A, 0, false);
        GameState state = createStateWithPhase(List.of(host), GamePhase.PLAYING_TRICK);
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));

        // When / Then
        thenThrownBy(() -> service.joinGame(new JoinGameCommand(GAME_ID, PSEUDO)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already started");
    }

    @Test
    @DisplayName("should throw when game is full with 4 players")
    void shouldThrowWhenGameIsFull() {
        // Given
        List<Player> fourPlayers = List.of(
            new Player(new PlayerId("p1"), "Alice", Team.TEAM_A, 0, false),
            new Player(new PlayerId("p2"), "Bob", Team.TEAM_B, 1, false),
            new Player(new PlayerId("p3"), "Charlie", Team.TEAM_A, 2, false),
            new Player(new PlayerId("p4"), "Diana", Team.TEAM_B, 3, false)
        );
        GameState state = createWaitingState(fourPlayers);
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));

        // When / Then
        thenThrownBy(() -> service.joinGame(new JoinGameCommand(GAME_ID, PSEUDO)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("full");
    }

    @Test
    @DisplayName("should alternate team assignment: seat 1 → TEAM_B, seat 2 → TEAM_A, seat 3 → TEAM_B")
    void shouldAlternateTeamAssignment() {
        // Given — 1 player already in game (seat 0, TEAM_A)
        var host = new Player(new PlayerId("host-id"), "Alice", Team.TEAM_A, 0, false);
        GameState state = createWaitingState(List.of(host));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class)))
            .willReturn(dummyView());

        // When — seat 1 joins
        JoinGameResult result = service.joinGame(new JoinGameCommand(GAME_ID, "Bob"));

        // Then — seat 1 should be TEAM_B
        then(result.players().get(1).team()).isEqualTo(Team.TEAM_B.name());

        // Given — now 2 players in game, prepare for seat 2
        List<Player> twoPlayers = List.of(
            host,
            new Player(new PlayerId("p2"), "Bob", Team.TEAM_B, 1, false)
        );
        GameState stateWith2 = createWaitingState(twoPlayers);
        given(repository.findById(GAME_ID)).willReturn(Optional.of(stateWith2));

        // When — seat 2 joins
        JoinGameResult result2 = service.joinGame(new JoinGameCommand(GAME_ID, "Charlie"));

        // Then — seat 2 should be TEAM_A (even index)
        then(result2.players().get(2).team()).isEqualTo(Team.TEAM_A.name());

        // Given — now 3 players, prepare for seat 3
        List<Player> threePlayers = List.of(
            host,
            new Player(new PlayerId("p2"), "Bob", Team.TEAM_B, 1, false),
            new Player(new PlayerId("p3"), "Charlie", Team.TEAM_A, 2, false)
        );
        GameState stateWith3 = createWaitingState(threePlayers);
        given(repository.findById(GAME_ID)).willReturn(Optional.of(stateWith3));

        // When — seat 3 joins (4th player)
        JoinGameResult result3 = service.joinGame(new JoinGameCommand(GAME_ID, "Diana"));

        // Then — seat 3 should be TEAM_B (odd index)
        then(result3.players().get(3).team()).isEqualTo(Team.TEAM_B.name());
    }

    @Test
    @DisplayName("should not auto-start game when fourth player joins")
    void shouldNotAutoStartGameWhenFourthPlayerJoins() {
        // Given — 3 players already present
        List<Player> threePlayers = List.of(
            new Player(new PlayerId("p1"), "Alice", Team.TEAM_A, 0, false),
            new Player(new PlayerId("p2"), "Bob", Team.TEAM_B, 1, false),
            new Player(new PlayerId("p3"), "Charlie", Team.TEAM_A, 2, false)
        );
        GameState waitingState = createWaitingState(threePlayers);
        given(repository.findById(GAME_ID)).willReturn(Optional.of(waitingState));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class)))
            .willReturn(dummyView());

        // When
        service.joinGame(new JoinGameCommand(GAME_ID, "Diana"));

        // Then — game should NOT auto-start; engine.startNewRound() should NOT be called
        verify(engine, never()).startNewRound(any(GameState.class));
        // But views should still be published to all 4 players
        verify(publisher, times(4)).publishGameView(
            eq(GAME_ID), any(PlayerId.class), any(GameView.class));
    }

    // --- helpers ---

    private GameState createWaitingState(List<Player> players) {
        Map<PlayerId, Hand> hands = new HashMap<>();
        players.forEach(p -> hands.put(p.id(), Hand.empty()));
        return new GameState(
            GAME_ID, GamePhase.WAITING_FOR_PLAYERS, players,
            null, null, hands, List.of(), List.of(), Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            0, false, null, Set.of()
        );
    }

    private GameState createStateWithPhase(List<Player> players, GamePhase phase) {
        Map<PlayerId, Hand> hands = new HashMap<>();
        players.forEach(p -> hands.put(p.id(), Hand.empty()));
        return new GameState(
            GAME_ID, phase, players,
            null, null, hands, List.of(), List.of(), Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            0, false, null, Set.of()
        );
    }

    private GameView dummyView() {
        return new GameView(GAME_ID, "WAITING_FOR_PLAYERS", List.of(), "TEAM_A",
            null, List.of(), List.of(), List.of(), null, Map.of(), List.of(), 0, false, null,
            List.of(), List.of(), "host-id");
    }
}
