package com.trutgame.server.application.dto;

public record AddAiPlayerCommand(String gameId, String requestingPlayerId) {
    public AddAiPlayerCommand {
        if (gameId == null || gameId.isBlank()) throw new IllegalArgumentException("GameId required");
        if (requestingPlayerId == null || requestingPlayerId.isBlank()) throw new IllegalArgumentException("RequestingPlayerId required");
    }
}
