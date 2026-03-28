package com.trutgame.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;

@DisplayName("Trick — unit tests")
class TrickTest {

    private static final PlayerId P1 = new PlayerId("p1");
    private static final PlayerId P2 = new PlayerId("p2");
    private static final PlayerId P3 = new PlayerId("p3");
    private static final PlayerId P4 = new PlayerId("p4");

    private static final Player PLAYER_1 = new Player(P1, "Alice", Team.TEAM_A, 0, false);
    private static final Player PLAYER_2 = new Player(P2, "Bob", Team.TEAM_B, 1, false);
    private static final Player PLAYER_3 = new Player(P3, "Charlie", Team.TEAM_A, 2, false);
    private static final Player PLAYER_4 = new Player(P4, "Diana", Team.TEAM_B, 3, false);
    private static final List<Player> ALL_PLAYERS = List.of(PLAYER_1, PLAYER_2, PLAYER_3, PLAYER_4);

    private static final Card SEVEN_HEARTS = new Card(CardValue.SEVEN, Suit.HEARTS);
    private static final Card EIGHT_DIAMONDS = new Card(CardValue.EIGHT, Suit.DIAMONDS);
    private static final Card ACE_CLUBS = new Card(CardValue.ACE, Suit.CLUBS);
    private static final Card NINE_SPADES = new Card(CardValue.NINE, Suit.SPADES);
    private static final Card KING_HEARTS = new Card(CardValue.KING, Suit.HEARTS);
    private static final Card SEVEN_DIAMONDS = new Card(CardValue.SEVEN, Suit.DIAMONDS);
    private static final Card QUEEN_CLUBS = new Card(CardValue.QUEEN, Suit.CLUBS);
    private static final Card TEN_SPADES = new Card(CardValue.TEN, Suit.SPADES);

    @Nested
    @DisplayName("empty()")
    class EmptyTrick {

        @Test
        @DisplayName("should create trick with no entries")
        void shouldCreateTrickWithNoEntries() {
            // When
            Trick trick = Trick.empty();

            // Then
            then(trick.entries()).isEmpty();
            then(trick.size()).isZero();
        }

        @Test
        @DisplayName("should not be complete with any player count")
        void shouldNotBeCompleteWithAnyPlayerCount() {
            then(Trick.empty().isComplete(4)).isFalse();
        }

        @Test
        @DisplayName("should have no winner")
        void shouldHaveNoWinner() {
            then(Trick.empty().winner(ALL_PLAYERS)).isEmpty();
        }
    }

    @Nested
    @DisplayName("addEntry()")
    class AddEntry {

        @Test
        @DisplayName("should add entry and increase size")
        void shouldAddEntryAndIncreaseSize() {
            // Given
            Trick trick = Trick.empty();

            // When
            Trick updated = trick.addEntry(new TrickEntry(P1, SEVEN_HEARTS));

            // Then
            then(updated.size()).isEqualTo(1);
            then(updated.entries()).hasSize(1);
        }

        @Test
        @DisplayName("should not modify original trick")
        void shouldNotModifyOriginalTrick() {
            // Given
            Trick trick = Trick.empty();

            // When
            trick.addEntry(new TrickEntry(P1, SEVEN_HEARTS));

            // Then — original unchanged
            then(trick.size()).isZero();
        }

        @Test
        @DisplayName("should chain multiple entries")
        void shouldChainMultipleEntries() {
            // When
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, SEVEN_HEARTS))
                .addEntry(new TrickEntry(P2, EIGHT_DIAMONDS))
                .addEntry(new TrickEntry(P3, ACE_CLUBS))
                .addEntry(new TrickEntry(P4, NINE_SPADES));

            // Then
            then(trick.size()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("isComplete()")
    class IsComplete {

        @Test
        @DisplayName("should return true when entries match player count")
        void shouldReturnTrueWhenEntriesMatchPlayerCount() {
            // Given
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, SEVEN_HEARTS))
                .addEntry(new TrickEntry(P2, EIGHT_DIAMONDS))
                .addEntry(new TrickEntry(P3, ACE_CLUBS))
                .addEntry(new TrickEntry(P4, NINE_SPADES));

            // Then
            then(trick.isComplete(4)).isTrue();
        }

        @Test
        @DisplayName("should return false when entries fewer than player count")
        void shouldReturnFalseWhenEntriesFewerThanPlayerCount() {
            // Given
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, SEVEN_HEARTS))
                .addEntry(new TrickEntry(P2, EIGHT_DIAMONDS));

            // Then
            then(trick.isComplete(4)).isFalse();
        }
    }

    @Nested
    @DisplayName("winner()")
    class Winner {

        @Test
        @DisplayName("should return TEAM_A when they have the strongest card")
        void shouldReturnTeamAWhenTheyHaveStrongestCard() {
            // Given — P1(TEAM_A) plays SEVEN(rank1), P2(TEAM_B) plays NINE(rank8)
            //         P3(TEAM_A) plays ACE(rank3), P4(TEAM_B) plays EIGHT(rank2)
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, SEVEN_HEARTS))    // TEAM_A — rank 1 (strongest)
                .addEntry(new TrickEntry(P2, NINE_SPADES))     // TEAM_B — rank 8
                .addEntry(new TrickEntry(P3, ACE_CLUBS))       // TEAM_A — rank 3
                .addEntry(new TrickEntry(P4, EIGHT_DIAMONDS)); // TEAM_B — rank 2

            // When
            Optional<Team> winner = trick.winner(ALL_PLAYERS);

            // Then
            then(winner).contains(Team.TEAM_A);
        }

        @Test
        @DisplayName("should return TEAM_B when they have the strongest card")
        void shouldReturnTeamBWhenTheyHaveStrongestCard() {
            // Given — P2(TEAM_B) plays SEVEN(rank1), strongest card
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, NINE_SPADES))     // TEAM_A — rank 8
                .addEntry(new TrickEntry(P2, SEVEN_HEARTS))    // TEAM_B — rank 1 (strongest)
                .addEntry(new TrickEntry(P3, ACE_CLUBS))       // TEAM_A — rank 3
                .addEntry(new TrickEntry(P4, KING_HEARTS));    // TEAM_B — rank 4

            // When
            Optional<Team> winner = trick.winner(ALL_PLAYERS);

            // Then
            then(winner).contains(Team.TEAM_B);
        }

        @Test
        @DisplayName("should return empty (pourri) when both teams have same highest value")
        void shouldReturnEmptyWhenBothTeamsHaveSameHighestValue() {
            // Given — both teams' best card is SEVEN (rank 1)
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, SEVEN_HEARTS))    // TEAM_A — rank 1
                .addEntry(new TrickEntry(P2, SEVEN_DIAMONDS))  // TEAM_B — rank 1
                .addEntry(new TrickEntry(P3, QUEEN_CLUBS))     // TEAM_A — rank 5
                .addEntry(new TrickEntry(P4, TEN_SPADES));     // TEAM_B — rank 7

            // When
            Optional<Team> winner = trick.winner(ALL_PLAYERS);

            // Then — pourri!
            then(winner).isEmpty();
        }

        @Test
        @DisplayName("should handle trick with mixed card strengths")
        void shouldHandleTrickWithMixedCardStrengths() {
            // Given — TEAM_A best=ACE(3), TEAM_B best=EIGHT(2) → TEAM_B wins
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, ACE_CLUBS))       // TEAM_A — rank 3
                .addEntry(new TrickEntry(P2, EIGHT_DIAMONDS))  // TEAM_B — rank 2 (strongest)
                .addEntry(new TrickEntry(P3, KING_HEARTS))     // TEAM_A — rank 4
                .addEntry(new TrickEntry(P4, NINE_SPADES));    // TEAM_B — rank 8

            // When
            Optional<Team> winner = trick.winner(ALL_PLAYERS);

            // Then
            then(winner).contains(Team.TEAM_B);
        }

        @Test
        @DisplayName("should return empty for trick with no entries")
        void shouldReturnEmptyForTrickWithNoEntries() {
            then(Trick.empty().winner(ALL_PLAYERS)).isEmpty();
        }
    }

    @Nested
    @DisplayName("leadPlayer()")
    class LeadPlayer {

        @Test
        @DisplayName("should return player with highest card")
        void shouldReturnPlayerWithHighestCard() {
            // Given — P1 plays SEVEN (strongest)
            Trick trick = Trick.empty()
                .addEntry(new TrickEntry(P1, SEVEN_HEARTS))
                .addEntry(new TrickEntry(P2, NINE_SPADES))
                .addEntry(new TrickEntry(P3, ACE_CLUBS))
                .addEntry(new TrickEntry(P4, EIGHT_DIAMONDS));

            // When
            var leader = trick.leadPlayer(ALL_PLAYERS);

            // Then
            then(leader).contains(P1);
        }

        @Test
        @DisplayName("should return empty for empty trick")
        void shouldReturnEmptyForEmptyTrick() {
            then(Trick.empty().leadPlayer(ALL_PLAYERS)).isEmpty();
        }
    }
}
