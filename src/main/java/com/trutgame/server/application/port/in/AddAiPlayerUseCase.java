package com.trutgame.server.application.port.in;

import com.trutgame.server.application.dto.AddAiPlayerCommand;

public interface AddAiPlayerUseCase {
    void addAiPlayer(AddAiPlayerCommand command);
}
