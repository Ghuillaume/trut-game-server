package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.dto.StartGameCommand;
import com.trutgame.server.application.port.in.StartGameUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.TrutGameEngine;

public class StartGameService implements StartGameUseCase {

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final TrutGameEngine engine;
    private final GameViewBuilder viewBuilder;

    public StartGameService(GameSessionRepository repository, GameViewPublisher publisher,
                            TrutGameEngine engine, GameViewBuilder viewBuilder) {
        this.repository = repository;
        this.publisher = publisher;
        this.engine = engine;
        this.viewBuilder = viewBuilder;
    }

    @Override
    public void startGame(StartGameCommand command) {
        GameState state = repository.findById(command.gameId())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + command.gameId()));

        if (state.phase() != GamePhase.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game already started");
        }

        Player requester = state.players().stream()
            .filter(p -> p.id().value().equals(command.requestingPlayerId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Requesting player not found"));

        if (requester.seatIndex() != 0) {
            throw new IllegalStateException("Only the game creator can start the game");
        }

        if (state.players().size() < 4) {
            throw new IllegalStateException("Need 4 players to start the game");
        }

        GameState startedState = engine.startNewRound(state);
        repository.save(startedState);

        for (Player player : startedState.players()) {
            GameView view = viewBuilder.buildView(startedState, player.id());
            publisher.publishGameView(startedState.gameId(), player.id(), view);
        }
    }
}
