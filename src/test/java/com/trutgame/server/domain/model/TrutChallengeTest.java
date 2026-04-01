package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("TrutChallenge — unit tests")
class TrutChallengeTest {

    private static final PlayerId CHALLENGER_ID = new PlayerId("p1");
    private static final PlayerId RESPONDER_ID = new PlayerId("p2");
    private static final Team CHALLENGER_TEAM = Team.TEAM_A;

    @Test
    @DisplayName("should create a new challenge with empty responses")
    void shouldCreateNewChallengeWithEmptyResponses() {
        // When
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.TRUT);

        // Then
        then(challenge.challengerId()).isEqualTo(CHALLENGER_ID);
        then(challenge.challengerTeam()).isEqualTo(CHALLENGER_TEAM);
        then(challenge.challengeType()).isEqualTo(TrutChallenge.ChallengeType.TRUT);
        then(challenge.respondedPlayers()).isEmpty();
        then(challenge.accepted()).isFalse();
        then(challenge.resolved()).isFalse();
    }

    @Test
    @DisplayName("should be waiting for response when not resolved")
    void shouldBeWaitingForResponseWhenNotResolved() {
        // Given
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.TRUT);

        // Then
        then(challenge.isWaitingForResponse()).isTrue();
    }

    @Test
    @DisplayName("should add response and track responding player")
    void shouldAddResponseAndTrackRespondingPlayer() {
        // Given
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.TRUT);

        // When
        var updated = challenge.addResponse(RESPONDER_ID, false);

        // Then
        then(updated.respondedPlayers()).containsExactly(RESPONDER_ID);
        then(updated.accepted()).isFalse();
    }

    @Test
    @DisplayName("should mark challenge as accepted when player goes to see")
    void shouldMarkAsAcceptedWhenPlayerGoesToSee() {
        // Given
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.TRUT);

        // When
        var updated = challenge.addResponse(RESPONDER_ID, true);

        // Then
        then(updated.accepted()).isTrue();
    }

    @Test
    @DisplayName("should remain accepted after adding fold response")
    void shouldRemainAcceptedAfterFoldResponse() {
        // Given
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.TRUT)
            .addResponse(RESPONDER_ID, true);

        // When
        var updated = challenge.addResponse(new PlayerId("p3"), false);

        // Then
        then(updated.accepted()).isTrue();
        then(updated.respondedPlayers()).hasSize(2);
    }

    @Test
    @DisplayName("should resolve challenge")
    void shouldResolveChallenge() {
        // Given
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.TRUT);

        // When
        var resolved = challenge.resolve();

        // Then
        then(resolved.resolved()).isTrue();
        then(resolved.isWaitingForResponse()).isFalse();
    }

    @Test
    @DisplayName("should support BRELLAN challenge type")
    void shouldSupportBrellanChallengeType() {
        // When
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.BRELLAN);

        // Then
        then(challenge.challengeType()).isEqualTo(TrutChallenge.ChallengeType.BRELLAN);
    }

    @Test
    @DisplayName("should support DEUX_PAREILLES challenge type")
    void shouldSupportDeuxPareillesChallengeType() {
        // When
        var challenge = TrutChallenge.create(CHALLENGER_ID, CHALLENGER_TEAM, TrutChallenge.ChallengeType.DEUX_PAREILLES);

        // Then
        then(challenge.challengeType()).isEqualTo(TrutChallenge.ChallengeType.DEUX_PAREILLES);
    }
}
