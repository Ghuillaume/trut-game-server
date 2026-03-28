package com.trutgame.server.domain.action;

import com.trutgame.server.domain.model.PlayerId;

public sealed interface GameAction permits PlayCardAction, TrutAction, CallAction, FoldAction, BrellanAction, DeuxPareillesAction {
    PlayerId playerId();
    String type();
}
