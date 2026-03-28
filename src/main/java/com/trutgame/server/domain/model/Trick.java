package com.trutgame.server.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Trick(List<TrickEntry> entries) {
    public Trick {
        entries = List.copyOf(entries);
    }

    public static Trick empty() {
        return new Trick(List.of());
    }

    public Trick addEntry(TrickEntry entry) {
        List<TrickEntry> newEntries = new ArrayList<>(entries);
        newEntries.add(entry);
        return new Trick(newEntries);
    }

    public boolean isComplete(int playerCount) {
        return entries.size() == playerCount;
    }

    public int size() {
        return entries.size();
    }

    /**
     * Returns the winning team, or empty if pourri (tie between teams).
     * In Trut: each team's best card is compared. If both teams play the same highest value, it's pourri.
     */
    public Optional<Team> winner(List<Player> players) {
        if (entries.isEmpty()) return Optional.empty();

        Card bestTeamA = null;
        Card bestTeamB = null;

        for (TrickEntry entry : entries) {
            Player player = players.stream()
                .filter(p -> p.id().equals(entry.playerId()))
                .findFirst().orElseThrow();

            if (player.team() == Team.TEAM_A) {
                if (bestTeamA == null || entry.card().value().rank() < bestTeamA.value().rank()) {
                    bestTeamA = entry.card();
                }
            } else {
                if (bestTeamB == null || entry.card().value().rank() < bestTeamB.value().rank()) {
                    bestTeamB = entry.card();
                }
            }
        }

        if (bestTeamA == null || bestTeamB == null) return Optional.empty();

        if (bestTeamA.value().rank() < bestTeamB.value().rank()) {
            return Optional.of(Team.TEAM_A);
        } else if (bestTeamB.value().rank() < bestTeamA.value().rank()) {
            return Optional.of(Team.TEAM_B);
        } else {
            return Optional.empty(); // Pourri!
        }
    }

    /**
     * Returns the PlayerId of the player who played the highest card.
     */
    public Optional<PlayerId> leadPlayer(List<Player> players) {
        if (entries.isEmpty()) return Optional.empty();

        TrickEntry best = entries.get(0);
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).card().value().rank() < best.card().value().rank()) {
                best = entries.get(i);
            }
        }
        return Optional.of(best.playerId());
    }
}
