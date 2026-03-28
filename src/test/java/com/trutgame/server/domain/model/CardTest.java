package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("Card — unit tests")
class CardTest {

    private static final Card SEVEN_HEARTS = new Card(CardValue.SEVEN, Suit.HEARTS);
    private static final Card NINE_SPADES = new Card(CardValue.NINE, Suit.SPADES);
    private static final Card ACE_DIAMONDS = new Card(CardValue.ACE, Suit.DIAMONDS);
    private static final Card ACE_CLUBS = new Card(CardValue.ACE, Suit.CLUBS);

    @Test
    @DisplayName("should format id as VALUE_SUIT")
    void shouldFormatIdAsValueSuit() {
        then(SEVEN_HEARTS.id()).isEqualTo("SEVEN_HEARTS");
    }

    @Test
    @DisplayName("should format id for NINE_SPADES")
    void shouldFormatIdForNineSpades() {
        then(NINE_SPADES.id()).isEqualTo("NINE_SPADES");
    }

    @Test
    @DisplayName("should format id for ACE_DIAMONDS")
    void shouldFormatIdForAceDiamonds() {
        then(ACE_DIAMONDS.id()).isEqualTo("ACE_DIAMONDS");
    }

    @Test
    @DisplayName("should parse card from id string")
    void shouldParseCardFromIdString() {
        // When
        Card card = Card.fromId("SEVEN_HEARTS");

        // Then
        then(card.value()).isEqualTo(CardValue.SEVEN);
        then(card.suit()).isEqualTo(Suit.HEARTS);
    }

    @Test
    @DisplayName("should parse NINE_SPADES from id")
    void shouldParseNineSpadesFromId() {
        // When
        Card card = Card.fromId("NINE_SPADES");

        // Then
        then(card).isEqualTo(NINE_SPADES);
    }

    @Test
    @DisplayName("should roundtrip through id and fromId")
    void shouldRoundtripThroughIdAndFromId() {
        // Given
        Card original = new Card(CardValue.KING, Suit.CLUBS);

        // When
        Card parsed = Card.fromId(original.id());

        // Then
        then(parsed).isEqualTo(original);
    }

    @Test
    @DisplayName("should compare stronger card as less than weaker card")
    void shouldCompareStrongerCardAsLessThanWeakerCard() {
        // SEVEN (rank 1) < NINE (rank 8)
        then(SEVEN_HEARTS.compareTo(NINE_SPADES)).isNegative();
    }

    @Test
    @DisplayName("should compare weaker card as greater than stronger card")
    void shouldCompareWeakerCardAsGreaterThanStrongerCard() {
        then(NINE_SPADES.compareTo(SEVEN_HEARTS)).isPositive();
    }

    @Test
    @DisplayName("should compare cards of same value as equal regardless of suit")
    void shouldCompareCardsOfSameValueAsEqualRegardlessOfSuit() {
        then(ACE_DIAMONDS.compareTo(ACE_CLUBS)).isZero();
    }

    @ParameterizedTest
    @DisplayName("should maintain consistent ordering across all values")
    @MethodSource("provideCardPairsForOrdering")
    void shouldMaintainConsistentOrderingAcrossAllValues(Card stronger, Card weaker) {
        then(stronger.compareTo(weaker)).isNegative();
        then(weaker.compareTo(stronger)).isPositive();
    }

    static Stream<Arguments> provideCardPairsForOrdering() {
        return Stream.of(
            Arguments.of(new Card(CardValue.SEVEN, Suit.HEARTS), new Card(CardValue.EIGHT, Suit.HEARTS)),
            Arguments.of(new Card(CardValue.EIGHT, Suit.HEARTS), new Card(CardValue.ACE, Suit.HEARTS)),
            Arguments.of(new Card(CardValue.ACE, Suit.HEARTS), new Card(CardValue.KING, Suit.HEARTS)),
            Arguments.of(new Card(CardValue.KING, Suit.HEARTS), new Card(CardValue.QUEEN, Suit.HEARTS)),
            Arguments.of(new Card(CardValue.QUEEN, Suit.HEARTS), new Card(CardValue.JACK, Suit.HEARTS)),
            Arguments.of(new Card(CardValue.JACK, Suit.HEARTS), new Card(CardValue.TEN, Suit.HEARTS)),
            Arguments.of(new Card(CardValue.TEN, Suit.HEARTS), new Card(CardValue.NINE, Suit.HEARTS))
        );
    }

    @Test
    @DisplayName("should expose value and suit via accessors")
    void shouldExposeValueAndSuitViaAccessors() {
        then(SEVEN_HEARTS.value()).isEqualTo(CardValue.SEVEN);
        then(SEVEN_HEARTS.suit()).isEqualTo(Suit.HEARTS);
    }
}
