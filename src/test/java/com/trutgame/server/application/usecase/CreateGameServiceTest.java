package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.CreateGameCommand;
import com.trutgame.server.application.dto.CreateGameResult;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.phase.GamePhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateGameService — unit tests")
class CreateGameServiceTest {

    private static final String PSEUDO = "Alice";

    @Mock
    private GameSessionRepository repository;

    @InjectMocks
    private CreateGameService service;

    @Test
    @DisplayName("should create game with unique gameId and playerId")
    void shouldCreateGameWithUniqueIdAndPlayerId() {
        // Given
        var command = new CreateGameCommand(PSEUDO);

        // When
        CreateGameResult result = service.createGame(command);

        // Then
        then(result.gameId()).isNotNull().isNotBlank();
        then(result.playerId()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("should save game state to repository")
    void shouldSaveGameToRepository() {
        // Given
        var command = new CreateGameCommand(PSEUDO);

        // When
        service.createGame(command);

        // Then
        ArgumentCaptor<GameState> captor = ArgumentCaptor.forClass(GameState.class);
        verify(repository).save(captor.capture());
        GameState saved = captor.getValue();
        then(saved).isNotNull();
        then(saved.gameId()).isNotBlank();
        then(saved.players()).hasSize(1);
    }

    @Test
    @DisplayName("should set phase to WAITING_FOR_PLAYERS")
    void shouldSetPhaseToWaitingForPlayers() {
        // Given
        var command = new CreateGameCommand(PSEUDO);

        // When
        service.createGame(command);

        // Then
        ArgumentCaptor<GameState> captor = ArgumentCaptor.forClass(GameState.class);
        verify(repository).save(captor.capture());
        then(captor.getValue().phase()).isEqualTo(GamePhase.WAITING_FOR_PLAYERS);
    }

    @Test
    @DisplayName("should assign host player to TEAM_A with seat index 0")
    void shouldAssignHostToTeamA() {
        // Given
        var command = new CreateGameCommand(PSEUDO);

        // When
        service.createGame(command);

        // Then
        ArgumentCaptor<GameState> captor = ArgumentCaptor.forClass(GameState.class);
        verify(repository).save(captor.capture());
        var host = captor.getValue().players().get(0);
        then(host.pseudo()).isEqualTo(PSEUDO);
        then(host.team()).isEqualTo(Team.TEAM_A);
        then(host.seatIndex()).isZero();
    }
}
