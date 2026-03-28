package com.trutgame.server.application.dto;

public record ActionCommand(String gameId, String playerId, String actionType, String cardId) {}
