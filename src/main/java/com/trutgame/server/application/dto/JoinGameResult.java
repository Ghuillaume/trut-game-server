package com.trutgame.server.application.dto;

import java.util.List;

public record JoinGameResult(String playerId, List<PlayerInfo> players) {
    public record PlayerInfo(String id, String pseudo, String team) {}
}
