package com.trutgame.server.domain.model;

public enum CardValue {
    SEVEN(1), EIGHT(2), ACE(3), KING(4), QUEEN(5), JACK(6), TEN(7), NINE(8);

    private final int rank;

    CardValue(int rank) { this.rank = rank; }

    public int rank() { return rank; }

    public boolean isStrongerThan(CardValue other) {
        return this.rank < other.rank;
    }
}
