package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("Team — unit tests")
class TeamTest {

    @Test
    @DisplayName("should have exactly two values")
    void shouldHaveExactlyTwoValues() {
        then(Team.values()).hasSize(2);
    }

    @Test
    @DisplayName("should contain TEAM_A and TEAM_B")
    void shouldContainTeamAAndTeamB() {
        then(Team.values()).containsExactly(Team.TEAM_A, Team.TEAM_B);
    }

    @Test
    @DisplayName("should return TEAM_B as opponent of TEAM_A")
    void shouldReturnTeamBAsOpponentOfTeamA() {
        then(Team.TEAM_A.opponent()).isEqualTo(Team.TEAM_B);
    }

    @Test
    @DisplayName("should return TEAM_A as opponent of TEAM_B")
    void shouldReturnTeamAAsOpponentOfTeamB() {
        then(Team.TEAM_B.opponent()).isEqualTo(Team.TEAM_A);
    }

    @Test
    @DisplayName("should return original team when opponent is called twice")
    void shouldReturnOriginalTeamWhenOpponentCalledTwice() {
        then(Team.TEAM_A.opponent().opponent()).isEqualTo(Team.TEAM_A);
        then(Team.TEAM_B.opponent().opponent()).isEqualTo(Team.TEAM_B);
    }
}
