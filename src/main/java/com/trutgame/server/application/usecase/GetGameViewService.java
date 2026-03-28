package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.in.GetGameViewUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.PlayerId;

public class GetGameViewService implements GetGameViewUseCase {

    private final GameSessionRepository repository;
    private final GameViewBuilder viewBuilder;

    public GetGameViewService(GameSessionRepository repository, GameViewBuilder viewBuilder) {
        this.repository = repository;
        this.viewBuilder = viewBuilder;
    }

    @Override
    public GameView getGameView(String gameId, PlayerId playerId) {
        GameState state = repository.findById(gameId)
            .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        return viewBuilder.buildView(state, playerId);
    }
}
