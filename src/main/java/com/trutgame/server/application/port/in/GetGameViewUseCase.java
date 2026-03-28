package com.trutgame.server.application.port.in;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.domain.model.PlayerId;

public interface GetGameViewUseCase {
    GameView getGameView(String gameId, PlayerId playerId);
}
