package com.trutgame.server.domain.action;

import com.trutgame.server.domain.model.PlayerId;

public record DeuxPareillesAction(PlayerId playerId) implements GameAction {
    @Override
    public String type() { return "DEUX_PAREILLES"; }
}
