package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("TokenCount — unit tests")
class TokenCountTest {

    @Nested
    @DisplayName("zero()")
    class Zero {

        @Test
        @DisplayName("should start at 0 grands and 0 petits")
        void shouldStartAtZeroGrandsAndZeroPetits() {
            // When
            TokenCount tokens = TokenCount.zero();

            // Then
            then(tokens.grands()).isZero();
            then(tokens.petits()).isZero();
        }
    }

    @Nested
    @DisplayName("addPetit()")
    class AddPetit {

        @Test
        @DisplayName("should increment petits by 1")
        void shouldIncrementPetitsBy1() {
            // Given
            TokenCount tokens = TokenCount.zero();

            // When
            TokenCount after = tokens.addPetit();

            // Then
            then(after.petits()).isEqualTo(1);
            then(after.grands()).isZero();
        }

        @Test
        @DisplayName("should increment petits to 2")
        void shouldIncrementPetitsTo2() {
            // Given
            TokenCount tokens = TokenCount.zero().addPetit();

            // When
            TokenCount after = tokens.addPetit();

            // Then
            then(after.petits()).isEqualTo(2);
            then(after.grands()).isZero();
        }

        @Test
        @DisplayName("should auto-convert 3 petits to 1 grand and reset petits to 0")
        void shouldAutoConvert3PetitsTo1GrandAndResetPetitsTo0() {
            // Given — 2 petits
            TokenCount tokens = TokenCount.zero().addPetit().addPetit();

            // When — adding a third petit
            TokenCount after = tokens.addPetit();

            // Then — converted to 1 grand, 0 petits
            then(after.grands()).isEqualTo(1);
            then(after.petits()).isZero();
        }

        @Test
        @DisplayName("should not modify original token count")
        void shouldNotModifyOriginalTokenCount() {
            // Given
            TokenCount tokens = TokenCount.zero();

            // When
            tokens.addPetit();

            // Then — original unchanged (records are immutable)
            then(tokens.petits()).isZero();
        }

        @Test
        @DisplayName("should accumulate grands through multiple conversions")
        void shouldAccumulateGrandsThroughMultipleConversions() {
            // Given — 1 grand from first conversion
            TokenCount tokens = TokenCount.zero().addPetit().addPetit().addPetit();
            then(tokens.grands()).isEqualTo(1);

            // When — another 3 petits
            tokens = tokens.addPetit().addPetit().addPetit();

            // Then — 2 grands
            then(tokens.grands()).isEqualTo(2);
            then(tokens.petits()).isZero();
        }
    }

    @Nested
    @DisplayName("addGrand()")
    class AddGrand {

        @Test
        @DisplayName("should increment grands by 1")
        void shouldIncrementGrandsBy1() {
            // When
            TokenCount after = TokenCount.zero().addGrand();

            // Then
            then(after.grands()).isEqualTo(1);
            then(after.petits()).isZero();
        }

        @Test
        @DisplayName("should not affect petits when adding grand")
        void shouldNotAffectPetitsWhenAddingGrand() {
            // Given — has 2 petits
            TokenCount tokens = TokenCount.zero().addPetit().addPetit();

            // When
            TokenCount after = tokens.addGrand();

            // Then
            then(after.grands()).isEqualTo(1);
            then(after.petits()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("losePetits()")
    class LosePetits {

        @Test
        @DisplayName("should reset petits to 0")
        void shouldResetPetitsToZero() {
            // Given — 2 petits
            TokenCount tokens = TokenCount.zero().addPetit().addPetit();

            // When
            TokenCount after = tokens.losePetits();

            // Then
            then(after.petits()).isZero();
        }

        @Test
        @DisplayName("should keep grands unchanged")
        void shouldKeepGrandsUnchanged() {
            // Given — 1 grand, 2 petits
            TokenCount tokens = new TokenCount(1, 2);

            // When
            TokenCount after = tokens.losePetits();

            // Then
            then(after.grands()).isEqualTo(1);
            then(after.petits()).isZero();
        }

        @Test
        @DisplayName("should be no-op when petits already 0")
        void shouldBeNoOpWhenPetitsAlready0() {
            // Given
            TokenCount tokens = new TokenCount(3, 0);

            // When
            TokenCount after = tokens.losePetits();

            // Then
            then(after.grands()).isEqualTo(3);
            then(after.petits()).isZero();
        }
    }

    @Nested
    @DisplayName("isFortial()")
    class IsFortial {

        @Test
        @DisplayName("should return true at exactly 6 grands and 2 petits")
        void shouldReturnTrueAtExactly6GrandsAnd2Petits() {
            // Given
            TokenCount tokens = new TokenCount(6, 2);

            // Then
            then(tokens.isFortial()).isTrue();
        }

        @Test
        @DisplayName("should return false at 6 grands and 1 petit")
        void shouldReturnFalseAt6GrandsAnd1Petit() {
            then(new TokenCount(6, 1).isFortial()).isFalse();
        }

        @Test
        @DisplayName("should return false at 6 grands and 0 petits")
        void shouldReturnFalseAt6GrandsAnd0Petits() {
            then(new TokenCount(6, 0).isFortial()).isFalse();
        }

        @Test
        @DisplayName("should return false at 5 grands and 2 petits")
        void shouldReturnFalseAt5GrandsAnd2Petits() {
            then(new TokenCount(5, 2).isFortial()).isFalse();
        }

        @Test
        @DisplayName("should return false at 7 grands")
        void shouldReturnFalseAt7Grands() {
            then(new TokenCount(7, 0).isFortial()).isFalse();
        }

        @Test
        @DisplayName("should return false at zero")
        void shouldReturnFalseAtZero() {
            then(TokenCount.zero().isFortial()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasWon()")
    class HasWon {

        @Test
        @DisplayName("should return true at exactly 7 grands")
        void shouldReturnTrueAtExactly7Grands() {
            then(new TokenCount(7, 0).hasWon()).isTrue();
        }

        @Test
        @DisplayName("should return true above 7 grands")
        void shouldReturnTrueAbove7Grands() {
            then(new TokenCount(8, 0).hasWon()).isTrue();
        }

        @Test
        @DisplayName("should return false at 6 grands")
        void shouldReturnFalseAt6Grands() {
            then(new TokenCount(6, 2).hasWon()).isFalse();
        }

        @Test
        @DisplayName("should return false at zero")
        void shouldReturnFalseAtZero() {
            then(TokenCount.zero().hasWon()).isFalse();
        }
    }

    @Nested
    @DisplayName("totalGrands()")
    class TotalGrands {

        @Test
        @DisplayName("should return grands value")
        void shouldReturnGrandsValue() {
            then(new TokenCount(5, 2).totalGrands()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Full scoring scenario")
    class FullScoringScenario {

        @Test
        @DisplayName("should reach 7 grands through 21 petits")
        void shouldReach7GrandsThrough21Petits() {
            // Given
            TokenCount tokens = TokenCount.zero();

            // When — add 21 petits (21/3 = 7 grands)
            for (int i = 0; i < 21; i++) {
                tokens = tokens.addPetit();
            }

            // Then
            then(tokens.grands()).isEqualTo(7);
            then(tokens.petits()).isZero();
            then(tokens.hasWon()).isTrue();
        }

        @Test
        @DisplayName("should reach fortial through petits and grands mix")
        void shouldReachFortialThroughPetitsAndGrandsMix() {
            // Given — 5 grands via addGrand, then 3 petits = 1 grand → 6 grands, then 2 petits
            TokenCount tokens = TokenCount.zero();
            for (int i = 0; i < 5; i++) {
                tokens = tokens.addGrand();
            }
            tokens = tokens.addPetit().addPetit().addPetit(); // → 6 grands, 0 petits
            tokens = tokens.addPetit().addPetit(); // → 6 grands, 2 petits

            // Then
            then(tokens.isFortial()).isTrue();
            then(tokens.hasWon()).isFalse();
        }

        @Test
        @DisplayName("should win from fortial with one more petit conversion")
        void shouldWinFromFortialWithOneMorePetitConversion() {
            // Given — at fortial (6 grands, 2 petits)
            TokenCount tokens = new TokenCount(6, 2);

            // When — one more petit triggers conversion to 7 grands
            tokens = tokens.addPetit();

            // Then
            then(tokens.grands()).isEqualTo(7);
            then(tokens.petits()).isZero();
            then(tokens.hasWon()).isTrue();
        }
    }
}
