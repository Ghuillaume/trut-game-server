package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.CreateGameCommand;
import com.trutgame.server.application.dto.CreateGameResult;
import com.trutgame.server.application.port.in.CreateGameUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.TokenCount;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.phase.GamePhase;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CreateGameService implements CreateGameUseCase {

    private final GameSessionRepository repository;

    public CreateGameService(GameSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public CreateGameResult createGame(CreateGameCommand command) {
        String gameId = UUID.randomUUID().toString();
        PlayerId playerId = PlayerId.generate();

        Player host = new Player(playerId, command.pseudo(), Team.TEAM_A, 0, false);

        GameState state = new GameState(
            gameId,
            GamePhase.WAITING_FOR_PLAYERS,
            List.of(host),
            null,
            null,
            Map.of(playerId, Hand.empty()),
            List.of(),
            List.of(),
            Trick.empty(),
            null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            0,
            false,
            null,
            Set.of()
        );

        repository.save(state);

        return new CreateGameResult(gameId, playerId.value());
    }
}
