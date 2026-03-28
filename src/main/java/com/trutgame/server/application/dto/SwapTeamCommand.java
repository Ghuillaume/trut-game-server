package com.trutgame.server.application.dto;

public record SwapTeamCommand(String gameId, String requestingPlayerId, String targetPlayerId) {
    public SwapTeamCommand {
        if (gameId == null || gameId.isBlank()) throw new IllegalArgumentException("GameId required");
        if (requestingPlayerId == null || requestingPlayerId.isBlank()) throw new IllegalArgumentException("RequestingPlayerId required");
        if (targetPlayerId == null || targetPlayerId.isBlank()) throw new IllegalArgumentException("TargetPlayerId required");
    }
}
