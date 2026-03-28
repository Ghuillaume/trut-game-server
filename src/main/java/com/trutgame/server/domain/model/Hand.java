package com.trutgame.server.domain.model;

import java.util.ArrayList;
import java.util.List;

public record Hand(List<Card> cards) {
    public Hand {
        cards = List.copyOf(cards);
    }

    public static Hand of(Card... cards) {
        return new Hand(List.of(cards));
    }

    public static Hand empty() {
        return new Hand(List.of());
    }

    public boolean contains(Card card) {
        return cards.contains(card);
    }

    public Hand remove(Card card) {
        List<Card> newCards = new ArrayList<>(cards);
        newCards.remove(card);
        return new Hand(newCards);
    }

    public int size() {
        return cards.size();
    }

    public boolean hasBrelan() {
        if (cards.size() != 3) return false;
        return cards.get(0).value() == cards.get(1).value()
            && cards.get(1).value() == cards.get(2).value();
    }

    public boolean hasDeuxPareillesUneFausse() {
        if (cards.size() < 2) return false;
        for (int i = 0; i < cards.size(); i++) {
            for (int j = i + 1; j < cards.size(); j++) {
                if (cards.get(i).value() == cards.get(j).value()) {
                    return true;
                }
            }
        }
        return false;
    }
}
