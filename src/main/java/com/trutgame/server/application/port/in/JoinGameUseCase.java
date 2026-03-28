package com.trutgame.server.application.port.in;

import com.trutgame.server.application.dto.JoinGameCommand;
import com.trutgame.server.application.dto.JoinGameResult;

public interface JoinGameUseCase {
    JoinGameResult joinGame(JoinGameCommand command);
}
