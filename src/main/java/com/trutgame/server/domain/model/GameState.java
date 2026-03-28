package com.trutgame.server.domain.model;

import com.trutgame.server.domain.phase.GamePhase;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record GameState(
    String gameId,
    GamePhase phase,
    List<Player> players,
    PlayerId currentDealerId,
    PlayerId currentPlayerId,
    Map<PlayerId, Hand> hands,
    List<Card> talon,
    List<Trick> completedTricks,
    Trick currentTrick,
    TrutChallenge trutChallenge,
    Map<Team, TokenCount> score,
    int roundNumber,
    boolean fortialActive,
    Team winner,
    Set<PlayerId> rematchVotes
) {
    public GameState {
        players = List.copyOf(players);
        hands = Map.copyOf(hands);
        talon = List.copyOf(talon);
        completedTricks = List.copyOf(completedTricks);
        score = Map.copyOf(score);
        rematchVotes = Set.copyOf(rematchVotes);
    }

    public Player getPlayer(PlayerId id) {
        return players.stream()
            .filter(p -> p.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Player not found: " + id));
    }

    public Team getTeam(PlayerId playerId) {
        return getPlayer(playerId).team();
    }

    public Player nextPlayer(PlayerId current) {
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).id().equals(current)) {
                currentIndex = i;
                break;
            }
        }
        return players.get((currentIndex + 1) % players.size());
    }

    public Player playerLeftOfDealer() {
        return nextPlayer(currentDealerId);
    }

    public boolean allPlayersPresent() {
        return players.size() == 4;
    }

    public List<Player> teamPlayers(Team team) {
        return players.stream().filter(p -> p.team() == team).toList();
    }
}
