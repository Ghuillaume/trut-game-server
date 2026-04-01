package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@DisplayName("PlayerId — unit tests")
class PlayerIdTest {

    private static final String PLAYER_VALUE = "player-42";

    @Test
    @DisplayName("should store and return the value")
    void shouldStoreAndReturnValue() {
        // Given
        var playerId = new PlayerId(PLAYER_VALUE);

        // Then
        then(playerId.value()).isEqualTo(PLAYER_VALUE);
    }

    @Test
    @DisplayName("should be equal to another PlayerId with same value")
    void shouldBeEqualToAnotherPlayerIdWithSameValue() {
        // Given
        var id1 = new PlayerId(PLAYER_VALUE);
        var id2 = new PlayerId(PLAYER_VALUE);

        // Then
        then(id1).isEqualTo(id2);
        then(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("should not be equal to PlayerId with different value")
    void shouldNotBeEqualToPlayerIdWithDifferentValue() {
        // Given
        var id1 = new PlayerId("a");
        var id2 = new PlayerId("b");

        // Then
        then(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("should include value in toString")
    void shouldIncludeValueInToString() {
        // Given
        var playerId = new PlayerId(PLAYER_VALUE);

        // Then
        then(playerId.toString()).contains(PLAYER_VALUE);
    }

    @Test
    @DisplayName("should throw when value is null")
    void shouldThrowWhenValueIsNull() {
        thenThrownBy(() -> new PlayerId(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw when value is blank")
    void shouldThrowWhenValueIsBlank() {
        thenThrownBy(() -> new PlayerId("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should generate unique ids")
    void shouldGenerateUniqueIds() {
        // When
        var id1 = PlayerId.generate();
        var id2 = PlayerId.generate();

        // Then
        then(id1).isNotEqualTo(id2);
        then(id1.value()).isNotBlank();
    }
}
