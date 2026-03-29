package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.ActionCommand;
import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.action.GameAction;
import com.trutgame.server.domain.action.PlayCardAction;
import com.trutgame.server.domain.model.Card;
import com.trutgame.server.domain.model.CardValue;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Suit;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.TokenCount;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.AiPlayerStrategy;
import com.trutgame.server.domain.service.TrutGameEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyActionService — unit tests")
class ApplyActionServiceTest {

    private static final String GAME_ID = "game-1";

    private static final PlayerId PLAYER_1_ID = new PlayerId("p1");
    private static final PlayerId PLAYER_2_ID = new PlayerId("p2");
    private static final PlayerId PLAYER_3_ID = new PlayerId("p3");
    private static final PlayerId PLAYER_4_ID = new PlayerId("p4");

    private static final Card ACE_HEARTS = new Card(CardValue.ACE, Suit.HEARTS);

    @Mock
    private GameSessionRepository repository;

    @Mock
    private GameViewPublisher publisher;

    @Mock
    private TrutGameEngine engine;

    @Mock
    private GameViewBuilder viewBuilder;

    @Mock
    private AiPlayerStrategy aiStrategy;

    @Mock
    private ScheduledExecutorService scheduler;

    @InjectMocks
    private ApplyActionService service;

    @Test
    @DisplayName("should apply a play card action via the engine and save state")
    void shouldApplyPlayCardAction() {
        // Given
        GameState state = createPlayingState();
        GameState newState = createPlayingState();
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(engine.apply(eq(state), any(PlayCardAction.class))).willReturn(newState);
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class)))
            .willReturn(dummyView());

        var command = new ActionCommand(GAME_ID, PLAYER_1_ID.value(), "PLAY_CARD", ACE_HEARTS.id());

        // When
        service.applyAction(command);

        // Then
        verify(engine).apply(eq(state), any(PlayCardAction.class));
        verify(repository).save(newState);
    }

    @Test
    @DisplayName("should throw when game is not found")
    void shouldThrowWhenGameNotFound() {
        // Given
        given(repository.findById(GAME_ID)).willReturn(Optional.empty());
        var command = new ActionCommand(GAME_ID, "p1", "PLAY_CARD", ACE_HEARTS.id());

        // When / Then
        thenThrownBy(() -> service.applyAction(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Game not found");
    }

    @Test
    @DisplayName("should publish game views to all players after action")
    void shouldPublishViewsToAllPlayers() {
        // Given
        GameState state = createPlayingState();
        GameState newState = createPlayingState();
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(engine.apply(eq(state), any(GameAction.class))).willReturn(newState);
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class)))
            .willReturn(dummyView());

        var command = new ActionCommand(GAME_ID, PLAYER_1_ID.value(), "PLAY_CARD", ACE_HEARTS.id());

        // When
        service.applyAction(command);

        // Then
        verify(publisher, times(4)).publishGameView(
            eq(GAME_ID), any(PlayerId.class), any(GameView.class));
    }

    @Test
    @DisplayName("should publish event message describing the action")
    void shouldPublishEventMessage() {
        // Given
        GameState state = createPlayingState();
        GameState newState = createPlayingState();
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(engine.apply(eq(state), any(GameAction.class))).willReturn(newState);
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class)))
            .willReturn(dummyView());

        var command = new ActionCommand(GAME_ID, PLAYER_1_ID.value(), "PLAY_CARD", ACE_HEARTS.id());

        // When
        service.applyAction(command);

        // Then
        verify(publisher).publishEvent(eq(GAME_ID), any(String.class));
    }

    // --- helpers ---

    private GameState createPlayingState() {
        List<Player> players = List.of(
            new Player(PLAYER_1_ID, "Alice", Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, "Bob", Team.TEAM_B, 1, false),
            new Player(PLAYER_3_ID, "Charlie", Team.TEAM_A, 2, false),
            new Player(PLAYER_4_ID, "Diana", Team.TEAM_B, 3, false)
        );
        Map<PlayerId, Hand> hands = Map.of(
            PLAYER_1_ID, Hand.of(ACE_HEARTS),
            PLAYER_2_ID, Hand.empty(),
            PLAYER_3_ID, Hand.empty(),
            PLAYER_4_ID, Hand.empty()
        );
        return new GameState(
            GAME_ID, GamePhase.PLAYING_TRICK, players,
            PLAYER_1_ID, PLAYER_1_ID, hands, List.of(), List.of(), Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            1, false, null, Set.of()
        );
    }

    private GameView dummyView() {
        return new GameView(GAME_ID, "PLAYING_TRICK", List.of(), "TEAM_A",
            null, List.of(), List.of(), List.of(), null, Map.of(), List.of(), 1, false, null,
            List.of(), List.of(), "p1");
    }
}
