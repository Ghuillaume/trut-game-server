package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("TrickEntry — unit tests")
class TrickEntryTest {

    private static final PlayerId PLAYER_ID = new PlayerId("p1");
    private static final Card ACE_HEARTS = new Card(CardValue.ACE, Suit.HEARTS);
    private static final Card SEVEN_SPADES = new Card(CardValue.SEVEN, Suit.SPADES);

    @Test
    @DisplayName("should expose playerId via accessor")
    void shouldExposePlayerId() {
        // Given
        var entry = new TrickEntry(PLAYER_ID, ACE_HEARTS);

        // Then
        then(entry.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    @DisplayName("should expose card via accessor")
    void shouldExposeCard() {
        // Given
        var entry = new TrickEntry(PLAYER_ID, ACE_HEARTS);

        // Then
        then(entry.card()).isEqualTo(ACE_HEARTS);
    }

    @Test
    @DisplayName("should be equal to another TrickEntry with same fields")
    void shouldBeEqualToAnotherTrickEntryWithSameFields() {
        // Given
        var e1 = new TrickEntry(PLAYER_ID, ACE_HEARTS);
        var e2 = new TrickEntry(PLAYER_ID, ACE_HEARTS);

        // Then
        then(e1).isEqualTo(e2);
        then(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when card differs")
    void shouldNotBeEqualWhenCardDiffers() {
        // Given
        var e1 = new TrickEntry(PLAYER_ID, ACE_HEARTS);
        var e2 = new TrickEntry(PLAYER_ID, SEVEN_SPADES);

        // Then
        then(e1).isNotEqualTo(e2);
    }

    @Test
    @DisplayName("should not be equal when playerId differs")
    void shouldNotBeEqualWhenPlayerIdDiffers() {
        // Given
        var e1 = new TrickEntry(new PlayerId("p1"), ACE_HEARTS);
        var e2 = new TrickEntry(new PlayerId("p2"), ACE_HEARTS);

        // Then
        then(e1).isNotEqualTo(e2);
    }
}
