package com.trutgame.server.domain.model;

public enum Team {
    TEAM_A, TEAM_B;

    public Team opponent() {
        return this == TEAM_A ? TEAM_B : TEAM_A;
    }
}
