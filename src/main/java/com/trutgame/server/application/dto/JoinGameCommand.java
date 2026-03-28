package com.trutgame.server.application.dto;

public record JoinGameCommand(String gameId, String pseudo) {
    public JoinGameCommand {
        if (gameId == null || gameId.isBlank()) throw new IllegalArgumentException("GameId required");
        if (pseudo == null || pseudo.isBlank()) throw new IllegalArgumentException("Pseudo required");
    }
}
