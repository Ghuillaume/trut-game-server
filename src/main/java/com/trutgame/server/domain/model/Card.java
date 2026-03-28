package com.trutgame.server.domain.model;

public record Card(CardValue value, Suit suit) implements Comparable<Card> {

    public String id() {
        return value.name() + "_" + suit.name();
    }

    public static Card fromId(String id) {
        String[] parts = id.split("_");
        return new Card(CardValue.valueOf(parts[0]), Suit.valueOf(parts[1]));
    }

    @Override
    public int compareTo(Card other) {
        return Integer.compare(this.value.rank(), other.value.rank());
    }
}
