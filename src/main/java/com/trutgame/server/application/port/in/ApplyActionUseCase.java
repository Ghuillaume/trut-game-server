package com.trutgame.server.application.port.in;

import com.trutgame.server.application.dto.ActionCommand;

public interface ApplyActionUseCase {
    void applyAction(ActionCommand command);
}
