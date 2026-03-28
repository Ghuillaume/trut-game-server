package com.trutgame.server.domain.model;

public record Player(PlayerId id, String pseudo, Team team, int seatIndex, boolean isAi) {
}
