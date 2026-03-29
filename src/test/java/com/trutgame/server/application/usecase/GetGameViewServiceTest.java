package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.out.GameSessionRepository;
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

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetGameViewService — unit tests")
class GetGameViewServiceTest {

    private static final String GAME_ID = "game-1";
    private static final PlayerId PLAYER_ID = new PlayerId("p1");

    @Mock
    private GameSessionRepository repository;

    @Mock
    private GameViewBuilder viewBuilder;

    @InjectMocks
    private GetGameViewService service;

    @Test
    @DisplayName("should return game view for a given player")
    void shouldReturnGameViewForPlayer() {
        // Given
        GameState state = createSimpleState();
        GameView expectedView = new GameView(
            GAME_ID, "WAITING_FOR_PLAYERS", List.of(), "TEAM_A",
            null, List.of(), List.of(), List.of(), null, Map.of(), List.of(), 0, false, null,
            List.of(), List.of(), null);
        given(repository.findById(GAME_ID)).willReturn(Optional.of(state));
        given(viewBuilder.buildView(state, PLAYER_ID)).willReturn(expectedView);

        // When
        GameView result = service.getGameView(GAME_ID, PLAYER_ID);

        // Then
        then(result).isEqualTo(expectedView);
        then(result.gameId()).isEqualTo(GAME_ID);
    }

    @Test
    @DisplayName("should throw when game is not found")
    void shouldThrowWhenGameNotFound() {
        // Given
        given(repository.findById(GAME_ID)).willReturn(Optional.empty());

        // When / Then
        thenThrownBy(() -> service.getGameView(GAME_ID, PLAYER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Game not found");
    }

    // --- helpers ---

    private GameState createSimpleState() {
        var player = new Player(PLAYER_ID, "Alice", Team.TEAM_A, 0, false);
        return new GameState(
            GAME_ID, GamePhase.WAITING_FOR_PLAYERS, List.of(player),
            null, null, Map.of(PLAYER_ID, Hand.empty()), List.of(), List.of(),
            Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            0, false, null, Set.of()
        );
    }
}
