package com.trutgame.server.application.port.in;

import com.trutgame.server.application.dto.StartGameCommand;

public interface StartGameUseCase {
    void startGame(StartGameCommand command);
}
