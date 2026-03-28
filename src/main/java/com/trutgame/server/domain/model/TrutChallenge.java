package com.trutgame.server.domain.model;

import java.util.ArrayList;
import java.util.List;

public record TrutChallenge(
    PlayerId challengerId,
    Team challengerTeam,
    ChallengeType challengeType,
    List<PlayerId> respondedPlayers,
    boolean accepted,
    boolean resolved
) {
    public enum ChallengeType { TRUT, BRELLAN, DEUX_PAREILLES }

    public TrutChallenge {
        respondedPlayers = List.copyOf(respondedPlayers);
    }

    public static TrutChallenge create(PlayerId challengerId, Team challengerTeam, ChallengeType type) {
        return new TrutChallenge(challengerId, challengerTeam, type, List.of(), false, false);
    }

    public TrutChallenge addResponse(PlayerId playerId, boolean goSee) {
        List<PlayerId> newResponded = new ArrayList<>(respondedPlayers);
        newResponded.add(playerId);
        boolean newAccepted = accepted || goSee;
        return new TrutChallenge(challengerId, challengerTeam, challengeType, newResponded, newAccepted, resolved);
    }

    public TrutChallenge resolve() {
        return new TrutChallenge(challengerId, challengerTeam, challengeType, respondedPlayers, accepted, true);
    }

    public boolean isWaitingForResponse() {
        return !resolved;
    }
}
