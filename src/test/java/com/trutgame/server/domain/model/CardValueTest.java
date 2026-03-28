package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("CardValue — unit tests")
class CardValueTest {

    @Test
    @DisplayName("should have exactly 8 card values")
    void shouldHaveExactlyEightCardValues() {
        then(CardValue.values()).hasSize(8);
    }

    @Test
    @DisplayName("should rank SEVEN as strongest with rank 1")
    void shouldRankSevenAsStrongestWithRank1() {
        then(CardValue.SEVEN.rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("should rank NINE as weakest with rank 8")
    void shouldRankNineAsWeakestWithRank8() {
        then(CardValue.NINE.rank()).isEqualTo(8);
    }

    @Test
    @DisplayName("should order all values from strongest to weakest")
    void shouldOrderAllValuesFromStrongestToWeakest() {
        then(CardValue.SEVEN.rank()).isEqualTo(1);
        then(CardValue.EIGHT.rank()).isEqualTo(2);
        then(CardValue.ACE.rank()).isEqualTo(3);
        then(CardValue.KING.rank()).isEqualTo(4);
        then(CardValue.QUEEN.rank()).isEqualTo(5);
        then(CardValue.JACK.rank()).isEqualTo(6);
        then(CardValue.TEN.rank()).isEqualTo(7);
        then(CardValue.NINE.rank()).isEqualTo(8);
    }

    @Test
    @DisplayName("should report SEVEN is stronger than EIGHT")
    void shouldReportSevenIsStrongerThanEight() {
        then(CardValue.SEVEN.isStrongerThan(CardValue.EIGHT)).isTrue();
    }

    @Test
    @DisplayName("should report SEVEN is stronger than NINE")
    void shouldReportSevenIsStrongerThanNine() {
        then(CardValue.SEVEN.isStrongerThan(CardValue.NINE)).isTrue();
    }

    @Test
    @DisplayName("should report NINE is not stronger than SEVEN")
    void shouldReportNineIsNotStrongerThanSeven() {
        then(CardValue.NINE.isStrongerThan(CardValue.SEVEN)).isFalse();
    }

    @Test
    @DisplayName("should report same value is not stronger than itself")
    void shouldReportSameValueIsNotStrongerThanItself() {
        then(CardValue.ACE.isStrongerThan(CardValue.ACE)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("should correctly compare all adjacent pairs")
    @MethodSource("provideAdjacentPairs")
    void shouldCorrectlyCompareAllAdjacentPairs(CardValue stronger, CardValue weaker) {
        then(stronger.isStrongerThan(weaker)).isTrue();
        then(weaker.isStrongerThan(stronger)).isFalse();
    }

    static Stream<Arguments> provideAdjacentPairs() {
        return Stream.of(
            Arguments.of(CardValue.SEVEN, CardValue.EIGHT),
            Arguments.of(CardValue.EIGHT, CardValue.ACE),
            Arguments.of(CardValue.ACE, CardValue.KING),
            Arguments.of(CardValue.KING, CardValue.QUEEN),
            Arguments.of(CardValue.QUEEN, CardValue.JACK),
            Arguments.of(CardValue.JACK, CardValue.TEN),
            Arguments.of(CardValue.TEN, CardValue.NINE)
        );
    }

    @Test
    @DisplayName("should contain all expected enum names")
    void shouldContainAllExpectedEnumNames() {
        then(CardValue.values())
            .extracting(CardValue::name)
            .containsExactly("SEVEN", "EIGHT", "ACE", "KING", "QUEEN", "JACK", "TEN", "NINE");
    }
}
