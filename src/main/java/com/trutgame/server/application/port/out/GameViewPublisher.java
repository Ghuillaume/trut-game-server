package com.trutgame.server.application.port.out;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.domain.model.PlayerId;

public interface GameViewPublisher {
    void publishGameView(String gameId, PlayerId playerId, GameView view);
    void publishEvent(String gameId, String eventMessage);
}
