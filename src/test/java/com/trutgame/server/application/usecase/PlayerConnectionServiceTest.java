package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerConnectionService — unit tests")
class PlayerConnectionServiceTest {

    private static final String GAME_ID = "game-1";
    private static final PlayerId PLAYER_1_ID = new PlayerId("p1");
    private static final PlayerId PLAYER_2_ID = new PlayerId("p2");
    private static final String PLAYER_1_PSEUDO = "Alice";
    private static final String PLAYER_2_PSEUDO = "Bob";

    @Mock
    private GameSessionRepository repository;

    @Mock
    private GameViewPublisher publisher;

    @Mock
    private GameViewBuilder viewBuilder;

    @InjectMocks
    private PlayerConnectionService service;

    @Test
    @DisplayName("should publish event with pseudo when player disconnects")
    void shouldPublishEventWithPseudoWhenPlayerDisconnects() {
        // Given
        GameState state = createState(List.of(
            new Player(PLAYER_1_ID, PLAYER_1_PSEUDO, Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, PLAYER_2_PSEUDO, Team.TEAM_B, 1, false)
        ));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class))).willReturn(dummyView());

        // When
        service.onPlayerDisconnected(GAME_ID, PLAYER_1_ID.value());

        // Then
        verify(publisher).publishEvent(GAME_ID, PLAYER_1_PSEUDO + " s'est déconnecté");
    }

    @Test
    @DisplayName("should publish event with pseudo when player reconnects")
    void shouldPublishEventWithPseudoWhenPlayerReconnects() {
        // Given
        GameState state = createState(List.of(
            new Player(PLAYER_1_ID, PLAYER_1_PSEUDO, Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, PLAYER_2_PSEUDO, Team.TEAM_B, 1, false)
        ));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class))).willReturn(dummyView());

        // When
        service.onPlayerConnected(GAME_ID, PLAYER_2_ID.value(), true);

        // Then
        verify(publisher).publishEvent(GAME_ID, PLAYER_2_PSEUDO + " s'est reconnecté !");
    }

    @Test
    @DisplayName("should not publish reconnect event on first connection")
    void shouldNotPublishReconnectEventOnFirstConnection() {
        // Given
        GameState state = createState(List.of(
            new Player(PLAYER_1_ID, PLAYER_1_PSEUDO, Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, PLAYER_2_PSEUDO, Team.TEAM_B, 1, false)
        ));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class))).willReturn(dummyView());

        // When
        service.onPlayerConnected(GAME_ID, PLAYER_2_ID.value(), false);

        // Then
        verify(publisher, never()).publishEvent(any(), any());
        verify(publisher, times(2)).publishGameView(eq(GAME_ID), any(PlayerId.class), any(GameView.class));
    }

    @Test
    @DisplayName("should publish game views to all players on disconnect")
    void shouldPublishGameViewsToAllPlayersOnDisconnect() {
        // Given
        GameState state = createState(List.of(
            new Player(PLAYER_1_ID, PLAYER_1_PSEUDO, Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, PLAYER_2_PSEUDO, Team.TEAM_B, 1, false)
        ));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class))).willReturn(dummyView());

        // When
        service.onPlayerDisconnected(GAME_ID, PLAYER_1_ID.value());

        // Then
        verify(publisher).publishGameView(eq(GAME_ID), eq(PLAYER_1_ID), any(GameView.class));
        verify(publisher).publishGameView(eq(GAME_ID), eq(PLAYER_2_ID), any(GameView.class));
        verify(publisher, times(2)).publishGameView(eq(GAME_ID), any(PlayerId.class), any(GameView.class));
    }

    @Test
    @DisplayName("should publish game views to all players on connect")
    void shouldPublishGameViewsToAllPlayersOnConnect() {
        // Given
        GameState state = createState(List.of(
            new Player(PLAYER_1_ID, PLAYER_1_PSEUDO, Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, PLAYER_2_PSEUDO, Team.TEAM_B, 1, false)
        ));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class))).willReturn(dummyView());

        // When
        service.onPlayerConnected(GAME_ID, PLAYER_1_ID.value(), true);

        // Then
        verify(publisher).publishGameView(eq(GAME_ID), eq(PLAYER_1_ID), any(GameView.class));
        verify(publisher).publishGameView(eq(GAME_ID), eq(PLAYER_2_ID), any(GameView.class));
        verify(publisher, times(2)).publishGameView(eq(GAME_ID), any(PlayerId.class), any(GameView.class));
    }

    @Test
    @DisplayName("should do nothing when game is not found")
    void shouldDoNothingWhenGameNotFound() {
        // Given
        given(repository.findById(GAME_ID)).willReturn(Optional.empty());

        // When
        service.onPlayerDisconnected(GAME_ID, PLAYER_1_ID.value());

        // Then
        verifyNoInteractions(publisher);
        verifyNoInteractions(viewBuilder);
    }

    @Test
    @DisplayName("should use fallback name when player is not in game")
    void shouldUseFallbackNameWhenPlayerNotInGame() {
        // Given
        GameState state = createState(List.of(
            new Player(PLAYER_1_ID, PLAYER_1_PSEUDO, Team.TEAM_A, 0, false)
        ));
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(any(GameState.class), any(PlayerId.class))).willReturn(dummyView());

        // When
        service.onPlayerDisconnected(GAME_ID, "unknown-player-id");

        // Then
        verify(publisher).publishEvent(GAME_ID, "Joueur inconnu s'est déconnecté");
    }

    // --- helpers ---

    private GameState createState(List<Player> players) {
        Map<PlayerId, Hand> hands = players.stream()
            .collect(java.util.stream.Collectors.toMap(Player::id, p -> Hand.empty()));
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
