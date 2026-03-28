package com.trutgame.server.application.port.in;

import com.trutgame.server.application.dto.SwapTeamCommand;

public interface SwapTeamUseCase {
    void swapTeam(SwapTeamCommand command);
}
