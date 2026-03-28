package com.trutgame.server.domain.action;

import com.trutgame.server.domain.model.Card;
import com.trutgame.server.domain.model.PlayerId;

public record PlayCardAction(PlayerId playerId, Card card) implements GameAction {
    @Override
    public String type() { return "PLAY_CARD"; }
}
