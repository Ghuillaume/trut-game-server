package com.trutgame.server.application.port.in;

import com.trutgame.server.application.dto.CreateGameCommand;
import com.trutgame.server.application.dto.CreateGameResult;

public interface CreateGameUseCase {
    CreateGameResult createGame(CreateGameCommand command);
}
