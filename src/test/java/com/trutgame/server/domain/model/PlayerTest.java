package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("Player — unit tests")
class PlayerTest {

    private static final PlayerId PLAYER_ID = new PlayerId("p1");
    private static final String PSEUDO = "Alice";
    private static final Team TEAM = Team.TEAM_A;
    private static final int SEAT_INDEX = 0;

    @Test
    @DisplayName("should expose id via accessor")
    void shouldExposeId() {
        // Given
        var player = new Player(PLAYER_ID, PSEUDO, TEAM, SEAT_INDEX, false);

        // Then
        then(player.id()).isEqualTo(PLAYER_ID);
    }

    @Test
    @DisplayName("should expose pseudo via accessor")
    void shouldExposePseudo() {
        // Given
        var player = new Player(PLAYER_ID, PSEUDO, TEAM, SEAT_INDEX, false);

        // Then
        then(player.pseudo()).isEqualTo(PSEUDO);
    }

    @Test
    @DisplayName("should expose team via accessor")
    void shouldExposeTeam() {
        // Given
        var player = new Player(PLAYER_ID, PSEUDO, TEAM, SEAT_INDEX, false);

        // Then
        then(player.team()).isEqualTo(TEAM);
    }

    @Test
    @DisplayName("should expose seat index via accessor")
    void shouldExposeSeatIndex() {
        // Given
        var player = new Player(PLAYER_ID, PSEUDO, TEAM, SEAT_INDEX, false);

        // Then
        then(player.seatIndex()).isEqualTo(SEAT_INDEX);
    }

    @Test
    @DisplayName("should report human player as not AI")
    void shouldReportHumanPlayerAsNotAi() {
        // Given
        var player = new Player(PLAYER_ID, PSEUDO, TEAM, SEAT_INDEX, false);

        // Then
        then(player.isAi()).isFalse();
    }

    @Test
    @DisplayName("should report AI player as AI")
    void shouldReportAiPlayerAsAi() {
        // Given
        var player = new Player(PLAYER_ID, "Bot", TEAM, SEAT_INDEX, true);

        // Then
        then(player.isAi()).isTrue();
    }

    @Test
    @DisplayName("should be equal to another Player with same fields")
    void shouldBeEqualToAnotherPlayerWithSameFields() {
        // Given
        var p1 = new Player(PLAYER_ID, PSEUDO, TEAM, SEAT_INDEX, false);
        var p2 = new Player(PLAYER_ID, PSEUDO, TEAM, SEAT_INDEX, false);

        // Then
        then(p1).isEqualTo(p2);
    }

    @Test
    @DisplayName("should not be equal when pseudo differs")
    void shouldNotBeEqualWhenPseudoDiffers() {
        // Given
        var p1 = new Player(PLAYER_ID, "Alice", TEAM, SEAT_INDEX, false);
        var p2 = new Player(PLAYER_ID, "Bob", TEAM, SEAT_INDEX, false);

        // Then
        then(p1).isNotEqualTo(p2);
    }
}
