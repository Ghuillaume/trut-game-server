package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.dto.StartGameCommand;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartGameService — unit tests")
class StartGameServiceTest {

    private static final String GAME_ID = "game-1";

    @Mock
    private GameSessionRepository repository;

    @Mock
    private GameViewPublisher publisher;

    @Mock
    private TrutGameEngine engine;

    @Mock
    private GameViewBuilder viewBuilder;

    @InjectMocks
    private StartGameService service;

    @Test
    @DisplayName("should start game when creator requests and 4 players present")
    void shouldStartGameWhenCreatorRequests() {
        // Given
        GameState state = createFullWaitingState();
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(engine.startNewRound(any(GameState.class))).willAnswer(inv -> inv.getArgument(0));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class)))
            .willReturn(dummyView());

        // When
        service.startGame(new StartGameCommand(GAME_ID, "p1"));

        // Then
        verify(engine).startNewRound(any(GameState.class));
        verify(repository).save(any(GameState.class));
        verify(publisher, times(4)).publishGameView(eq(GAME_ID), any(PlayerId.class), any(GameView.class));
    }

    @Test
    @DisplayName("should throw when non-creator tries to start")
    void shouldThrowWhenNonCreatorTriesToStart() {
        // Given
        GameState state = createFullWaitingState();
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));

        // When / Then
        thenThrownBy(() -> service.startGame(new StartGameCommand(GAME_ID, "p2")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only the game creator");
    }

    @Test
    @DisplayName("should throw when less than 4 players")
    void shouldThrowWhenLessThanFourPlayers() {
        // Given
        List<Player> twoPlayers = List.of(
            new Player(new PlayerId("p1"), "Alice", Team.TEAM_A, 0, false),
            new Player(new PlayerId("p2"), "Bob", Team.TEAM_B, 1, false)
        );
        GameState state = createWaitingState(twoPlayers);
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));

        // When / Then
        thenThrownBy(() -> service.startGame(new StartGameCommand(GAME_ID, "p1")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Need 4 players");
    }

    @Test
    @DisplayName("should throw when game already started")
    void shouldThrowWhenGameAlreadyStarted() {
        // Given
        List<Player> players = createFourPlayers();
        Map<PlayerId, Hand> hands = new HashMap<>();
        players.forEach(p -> hands.put(p.id(), Hand.empty()));
        GameState state = new GameState(
            GAME_ID, GamePhase.PLAYING_TRICK, players,
            null, null, hands, List.of(), List.of(), Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            1, false, null, Set.of()
        );
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));

        // When / Then
        thenThrownBy(() -> service.startGame(new StartGameCommand(GAME_ID, "p1")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already started");
    }

    @Test
    @DisplayName("should throw when game not found")
    void shouldThrowWhenGameNotFound() {
        // Given
        given(repository.findById(GAME_ID)).willReturn(Optional.empty());

        // When / Then
        thenThrownBy(() -> service.startGame(new StartGameCommand(GAME_ID, "p1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Game not found");
    }

    // --- helpers ---

    private List<Player> createFourPlayers() {
        return List.of(
            new Player(new PlayerId("p1"), "Alice", Team.TEAM_A, 0, false),
            new Player(new PlayerId("p2"), "Bob", Team.TEAM_B, 1, false),
            new Player(new PlayerId("p3"), "Charlie", Team.TEAM_A, 2, false),
            new Player(new PlayerId("p4"), "Diana", Team.TEAM_B, 3, false)
        );
    }

    private GameState createFullWaitingState() {
        return createWaitingState(createFourPlayers());
    }

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

    private GameView dummyView() {
        return new GameView(GAME_ID, "PLAYING_TRICK", List.of(), "TEAM_A",
            null, List.of(), List.of(), List.of(), null, Map.of(), List.of(), 1, false, null,
            List.of(), List.of(), "p1");
    }
}
