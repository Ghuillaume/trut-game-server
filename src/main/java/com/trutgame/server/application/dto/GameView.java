package com.trutgame.server.application.dto;

import java.util.List;
import java.util.Map;

public record GameView(
    String gameId,
    String phase,
    List<String> myHand,
    String myTeam,
    String currentPlayerId,
    List<PlayerView> players,
    List<TrickEntryView> currentTrick,
    List<CompletedTrickView> completedTricks,
    TrutChallengeView trutChallenge,
    Map<String, ScoreView> score,
    List<String> availableActions,
    int roundNumber,
    boolean fortial,
    String winner,
    List<String> rematchVotes,
    List<String> disconnectedPlayers,
    String creatorId
) {
    public record PlayerView(String id, String pseudo, String team, int cardCount, boolean isAi) {}
    public record TrickEntryView(String playerId, String card) {}
    public record CompletedTrickView(List<TrickEntryView> entries, String winnerTeam) {}
    public record TrutChallengeView(String challengerId, String status, String challengeType) {}
    public record ScoreView(int grands, int petits) {}
}
