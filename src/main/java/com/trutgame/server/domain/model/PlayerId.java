package com.trutgame.server.domain.model;

import java.util.UUID;

public record PlayerId(String value) {
    public PlayerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PlayerId cannot be null or blank");
        }
    }

    public static PlayerId generate() {
        return new PlayerId(UUID.randomUUID().toString());
    }
}
