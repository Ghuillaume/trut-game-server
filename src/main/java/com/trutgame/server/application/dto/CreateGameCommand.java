package com.trutgame.server.application.dto;

public record CreateGameCommand(String pseudo) {
    public CreateGameCommand {
        if (pseudo == null || pseudo.isBlank()) {
            throw new IllegalArgumentException("Pseudo cannot be null or blank");
        }
    }
}
