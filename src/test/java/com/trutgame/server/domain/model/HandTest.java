package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("Hand — unit tests")
class HandTest {

    private static final Card SEVEN_HEARTS = new Card(CardValue.SEVEN, Suit.HEARTS);
    private static final Card SEVEN_DIAMONDS = new Card(CardValue.SEVEN, Suit.DIAMONDS);
    private static final Card SEVEN_CLUBS = new Card(CardValue.SEVEN, Suit.CLUBS);
    private static final Card ACE_HEARTS = new Card(CardValue.ACE, Suit.HEARTS);
    private static final Card ACE_DIAMONDS = new Card(CardValue.ACE, Suit.DIAMONDS);
    private static final Card KING_SPADES = new Card(CardValue.KING, Suit.SPADES);
    private static final Card NINE_SPADES = new Card(CardValue.NINE, Suit.SPADES);

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("should create hand with of() factory")
        void shouldCreateHandWithOfFactory() {
            // When
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS, KING_SPADES);

            // Then
            then(hand.size()).isEqualTo(3);
            then(hand.cards()).containsExactly(SEVEN_HEARTS, ACE_HEARTS, KING_SPADES);
        }

        @Test
        @DisplayName("should create empty hand with empty() factory")
        void shouldCreateEmptyHandWithEmptyFactory() {
            // When
            Hand hand = Hand.empty();

            // Then
            then(hand.size()).isZero();
            then(hand.cards()).isEmpty();
        }

        @Test
        @DisplayName("should create hand with single card")
        void shouldCreateHandWithSingleCard() {
            // When
            Hand hand = Hand.of(SEVEN_HEARTS);

            // Then
            then(hand.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("contains()")
    class Contains {

        @Test
        @DisplayName("should return true when card is in hand")
        void shouldReturnTrueWhenCardIsInHand() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS, KING_SPADES);

            // Then
            then(hand.contains(SEVEN_HEARTS)).isTrue();
            then(hand.contains(ACE_HEARTS)).isTrue();
            then(hand.contains(KING_SPADES)).isTrue();
        }

        @Test
        @DisplayName("should return false when card is not in hand")
        void shouldReturnFalseWhenCardIsNotInHand() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS);

            // Then
            then(hand.contains(NINE_SPADES)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty hand")
        void shouldReturnFalseForEmptyHand() {
            then(Hand.empty().contains(SEVEN_HEARTS)).isFalse();
        }
    }

    @Nested
    @DisplayName("remove()")
    class Remove {

        @Test
        @DisplayName("should remove card and return new hand without it")
        void shouldRemoveCardAndReturnNewHandWithoutIt() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS, KING_SPADES);

            // When
            Hand after = hand.remove(SEVEN_HEARTS);

            // Then
            then(after.size()).isEqualTo(2);
            then(after.contains(SEVEN_HEARTS)).isFalse();
            then(after.contains(ACE_HEARTS)).isTrue();
            then(after.contains(KING_SPADES)).isTrue();
        }

        @Test
        @DisplayName("should not modify original hand on remove")
        void shouldNotModifyOriginalHandOnRemove() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS);

            // When
            hand.remove(SEVEN_HEARTS);

            // Then — original unchanged
            then(hand.size()).isEqualTo(2);
            then(hand.contains(SEVEN_HEARTS)).isTrue();
        }

        @Test
        @DisplayName("should return same-size hand when removing card not present")
        void shouldReturnSameSizeHandWhenRemovingCardNotPresent() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS);

            // When
            Hand after = hand.remove(NINE_SPADES);

            // Then
            then(after.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("size()")
    class Size {

        @Test
        @DisplayName("should return 0 for empty hand")
        void shouldReturnZeroForEmptyHand() {
            then(Hand.empty().size()).isZero();
        }

        @Test
        @DisplayName("should return 3 for three-card hand")
        void shouldReturnThreeForThreeCardHand() {
            then(Hand.of(SEVEN_HEARTS, ACE_HEARTS, KING_SPADES).size()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("hasBrelan()")
    class HasBrelan {

        @Test
        @DisplayName("should return true when all three cards have same value")
        void shouldReturnTrueWhenAllThreeCardsHaveSameValue() {
            // Given — three SEVEN cards
            Hand hand = Hand.of(SEVEN_HEARTS, SEVEN_DIAMONDS, SEVEN_CLUBS);

            // Then
            then(hand.hasBrelan()).isTrue();
        }

        @Test
        @DisplayName("should return false when cards have different values")
        void shouldReturnFalseWhenCardsHaveDifferentValues() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS, KING_SPADES);

            // Then
            then(hand.hasBrelan()).isFalse();
        }

        @Test
        @DisplayName("should return false when only two cards match")
        void shouldReturnFalseWhenOnlyTwoCardsMatch() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, SEVEN_DIAMONDS, ACE_HEARTS);

            // Then
            then(hand.hasBrelan()).isFalse();
        }

        @Test
        @DisplayName("should return false for hand with fewer than 3 cards")
        void shouldReturnFalseForHandWithFewerThan3Cards() {
            then(Hand.of(SEVEN_HEARTS, SEVEN_DIAMONDS).hasBrelan()).isFalse();
        }

        @Test
        @DisplayName("should return false for empty hand")
        void shouldReturnFalseForEmptyHand() {
            then(Hand.empty().hasBrelan()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasDeuxPareillesUneFausse()")
    class HasDeuxPareillesUneFausse {

        @Test
        @DisplayName("should return true when two cards share same value")
        void shouldReturnTrueWhenTwoCardsShareSameValue() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, SEVEN_DIAMONDS, ACE_HEARTS);

            // Then
            then(hand.hasDeuxPareillesUneFausse()).isTrue();
        }

        @Test
        @DisplayName("should return true when second and third cards match")
        void shouldReturnTrueWhenSecondAndThirdCardsMatch() {
            // Given
            Hand hand = Hand.of(KING_SPADES, ACE_HEARTS, ACE_DIAMONDS);

            // Then
            then(hand.hasDeuxPareillesUneFausse()).isTrue();
        }

        @Test
        @DisplayName("should return true for brelan as well since pairs exist")
        void shouldReturnTrueForBrelanAsWellSincePairsExist() {
            // Given — 3 matching is also 2 matching
            Hand hand = Hand.of(SEVEN_HEARTS, SEVEN_DIAMONDS, SEVEN_CLUBS);

            // Then
            then(hand.hasDeuxPareillesUneFausse()).isTrue();
        }

        @Test
        @DisplayName("should return false when all cards are different values")
        void shouldReturnFalseWhenAllCardsAreDifferentValues() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS, KING_SPADES);

            // Then
            then(hand.hasDeuxPareillesUneFausse()).isFalse();
        }

        @Test
        @DisplayName("should return false for single card hand")
        void shouldReturnFalseForSingleCardHand() {
            then(Hand.of(SEVEN_HEARTS).hasDeuxPareillesUneFausse()).isFalse();
        }

        @Test
        @DisplayName("should return false for empty hand")
        void shouldReturnFalseForEmptyHand() {
            then(Hand.empty().hasDeuxPareillesUneFausse()).isFalse();
        }

        @Test
        @DisplayName("should detect pair in two-card hand")
        void shouldDetectPairInTwoCardHand() {
            // Given
            Hand hand = Hand.of(ACE_HEARTS, ACE_DIAMONDS);

            // Then
            then(hand.hasDeuxPareillesUneFausse()).isTrue();
        }

        @Test
        @DisplayName("should not detect pair in two different cards")
        void shouldNotDetectPairInTwoDifferentCards() {
            // Given
            Hand hand = Hand.of(SEVEN_HEARTS, ACE_HEARTS);

            // Then
            then(hand.hasDeuxPareillesUneFausse()).isFalse();
        }
    }
}
