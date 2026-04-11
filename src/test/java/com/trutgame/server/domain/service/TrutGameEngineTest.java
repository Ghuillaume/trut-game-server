package com.trutgame.server.domain.service;

import com.trutgame.server.domain.action.*;
import com.trutgame.server.domain.exception.InvalidActionException;
import com.trutgame.server.domain.model.*;
import com.trutgame.server.domain.phase.GamePhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@DisplayName("TrutGameEngine — unit tests")
class TrutGameEngineTest {

    private static final PlayerId P1 = new PlayerId("p1");
    private static final PlayerId P2 = new PlayerId("p2");
    private static final PlayerId P3 = new PlayerId("p3");
    private static final PlayerId P4 = new PlayerId("p4");
    private static final String GAME_ID = "test-game";

    private static final Player PLAYER_1 = new Player(P1, "Alice", Team.TEAM_A, 0, false);
    private static final Player PLAYER_2 = new Player(P2, "Bob", Team.TEAM_B, 1, false);
    private static final Player PLAYER_3 = new Player(P3, "Charlie", Team.TEAM_A, 2, false);
    private static final Player PLAYER_4 = new Player(P4, "Diana", Team.TEAM_B, 3, false);
    private static final List<Player> ALL_PLAYERS = List.of(PLAYER_1, PLAYER_2, PLAYER_3, PLAYER_4);

    // ── Seating order tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("ensureAlternatingTeams")
    class EnsureAlternatingTeams {

        @Test
        @DisplayName("should keep already alternating order unchanged")
        void shouldKeepAlreadyAlternating() {
            // A, B, A, B → no change
            var result = TrutGameEngine.ensureAlternatingTeams(ALL_PLAYERS);
            then(result).hasSize(4);
            then(result.get(0).team()).isEqualTo(Team.TEAM_A);
            then(result.get(1).team()).isEqualTo(Team.TEAM_B);
            then(result.get(2).team()).isEqualTo(Team.TEAM_A);
            then(result.get(3).team()).isEqualTo(Team.TEAM_B);
        }

        @Test
        @DisplayName("should reorder when same team sits together")
        void shouldReorderWhenSameTeamTogether() {
            // A, A, B, B → should become A, B, A, B
            var badOrder = List.of(
                new Player(P1, "Alice", Team.TEAM_A, 0, false),
                new Player(P3, "Charlie", Team.TEAM_A, 1, false),
                new Player(P2, "Bob", Team.TEAM_B, 2, false),
                new Player(P4, "Diana", Team.TEAM_B, 3, false)
            );
            var result = TrutGameEngine.ensureAlternatingTeams(badOrder);
            then(result.get(0).team()).isEqualTo(Team.TEAM_A);
            then(result.get(1).team()).isEqualTo(Team.TEAM_B);
            then(result.get(2).team()).isEqualTo(Team.TEAM_A);
            then(result.get(3).team()).isEqualTo(Team.TEAM_B);
        }

        @Test
        @DisplayName("should preserve player identity when reordering")
        void shouldPreservePlayerIdentity() {
            var badOrder = List.of(
                new Player(P1, "Alice", Team.TEAM_A, 0, false),
                new Player(P3, "Charlie", Team.TEAM_A, 1, false),
                new Player(P2, "Bob", Team.TEAM_B, 2, false),
                new Player(P4, "Diana", Team.TEAM_B, 3, false)
            );
            var result = TrutGameEngine.ensureAlternatingTeams(badOrder);
            then(result.get(0).pseudo()).isEqualTo("Alice");
            then(result.get(1).pseudo()).isEqualTo("Bob");
            then(result.get(2).pseudo()).isEqualTo("Charlie");
            then(result.get(3).pseudo()).isEqualTo("Diana");
        }

        @Test
        @DisplayName("should update seatIndex after reordering")
        void shouldUpdateSeatIndex() {
            var badOrder = List.of(
                new Player(P1, "Alice", Team.TEAM_A, 0, false),
                new Player(P3, "Charlie", Team.TEAM_A, 1, false),
                new Player(P2, "Bob", Team.TEAM_B, 2, false),
                new Player(P4, "Diana", Team.TEAM_B, 3, false)
            );
            var result = TrutGameEngine.ensureAlternatingTeams(badOrder);
            for (int i = 0; i < result.size(); i++) {
                then(result.get(i).seatIndex()).isEqualTo(i);
            }
        }
    }

    private static final Card SEVEN_H = new Card(CardValue.SEVEN, Suit.HEARTS);
    private static final Card SEVEN_D = new Card(CardValue.SEVEN, Suit.DIAMONDS);
    private static final Card EIGHT_H = new Card(CardValue.EIGHT, Suit.HEARTS);
    private static final Card EIGHT_D = new Card(CardValue.EIGHT, Suit.DIAMONDS);
    private static final Card ACE_H = new Card(CardValue.ACE, Suit.HEARTS);
    private static final Card ACE_D = new Card(CardValue.ACE, Suit.DIAMONDS);
    private static final Card KING_H = new Card(CardValue.KING, Suit.HEARTS);
    private static final Card KING_D = new Card(CardValue.KING, Suit.DIAMONDS);
    private static final Card QUEEN_H = new Card(CardValue.QUEEN, Suit.HEARTS);
    private static final Card QUEEN_D = new Card(CardValue.QUEEN, Suit.DIAMONDS);
    private static final Card JACK_H = new Card(CardValue.JACK, Suit.HEARTS);
    private static final Card JACK_D = new Card(CardValue.JACK, Suit.DIAMONDS);
    private static final Card TEN_H = new Card(CardValue.TEN, Suit.HEARTS);
    private static final Card TEN_D = new Card(CardValue.TEN, Suit.DIAMONDS);
    private static final Card NINE_H = new Card(CardValue.NINE, Suit.HEARTS);
    private static final Card NINE_D = new Card(CardValue.NINE, Suit.DIAMONDS);
    private static final Card NINE_C = new Card(CardValue.NINE, Suit.CLUBS);
    private static final Card NINE_S = new Card(CardValue.NINE, Suit.SPADES);

    private final TrutGameEngine engine = new TrutGameEngine();

    // Dealer=P1, currentPlayer=P2 (left of dealer)
    // Clockwise: P2→P3→P4→P1. TEAM_A responders: P3 first, then P1
    private GameState createPlayingState(Map<PlayerId, Hand> hands) {
        return new GameState(GAME_ID, GamePhase.PLAYING_TRICK, ALL_PLAYERS,
                P1, P2, hands, List.of(), List.of(), Trick.empty(), null,
                Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
                1, false, null, Set.of());
    }

    private Map<PlayerId, Hand> defaultHands() {
        return new HashMap<>(Map.of(
                P1, Hand.of(SEVEN_H, KING_H, NINE_H),
                P2, Hand.of(EIGHT_H, QUEEN_H, TEN_H),
                P3, Hand.of(ACE_H, JACK_H, NINE_D),
                P4, Hand.of(ACE_D, KING_D, NINE_C)
        ));
    }

    @Nested
    @DisplayName("Deck building")
    class DeckBuilding {
        @Test @DisplayName("should build deck with 32 cards")
        void shouldBuildDeckWith32Cards() {
            then(TrutGameEngine.buildDeck()).hasSize(32);
        }

        @Test @DisplayName("should contain all suits and values")
        void shouldContainAllSuitsAndValues() {
            List<Card> deck = TrutGameEngine.buildDeck();
            for (Suit s : Suit.values())
                for (CardValue v : CardValue.values())
                    then(deck).contains(new Card(v, s));
        }
    }

    @Nested
    @DisplayName("Starting a round")
    class StartingARound {
        @Test @DisplayName("should deal three cards each")
        void shouldDealThreeCardsEach() {
            GameState s = engine.startNewRound(TrutGameEngine.createInitialState(GAME_ID, ALL_PLAYERS));
            for (Player p : ALL_PLAYERS) then(s.hands().get(p.id()).size()).isEqualTo(3);
        }

        @Test @DisplayName("should set current player to left of dealer")
        void shouldSetCurrentPlayerLeftOfDealer() {
            GameState s = engine.startNewRound(TrutGameEngine.createInitialState(GAME_ID, ALL_PLAYERS));
            then(s.currentPlayerId()).isEqualTo(P2);
        }
    }

    @Nested
    @DisplayName("Playing cards")
    class PlayingCards {
        @Test @DisplayName("should play card and advance to next player")
        void shouldPlayCardAndAdvanceToNextPlayer() {
            GameState after = engine.apply(createPlayingState(defaultHands()), new PlayCardAction(P2, EIGHT_H));
            then(after.currentPlayerId()).isEqualTo(P3);
            then(after.hands().get(P2).size()).isEqualTo(2);
            then(after.currentTrick().size()).isEqualTo(1);
        }

        @Test @DisplayName("should reject card not in hand")
        void shouldRejectCardNotInHand() {
            thenThrownBy(() -> engine.apply(createPlayingState(defaultHands()), new PlayCardAction(P2, SEVEN_H)))
                    .isInstanceOf(InvalidActionException.class);
        }

        @Test @DisplayName("should reject play when not player turn")
        void shouldRejectPlayWhenNotPlayerTurn() {
            thenThrownBy(() -> engine.apply(createPlayingState(defaultHands()), new PlayCardAction(P3, ACE_H)))
                    .isInstanceOf(InvalidActionException.class);
        }

        @Test @DisplayName("should reject play in wrong phase")
        void shouldRejectPlayInWrongPhase() {
            thenThrownBy(() -> engine.apply(TrutGameEngine.createInitialState(GAME_ID, ALL_PLAYERS), new PlayCardAction(P1, SEVEN_H)))
                    .isInstanceOf(InvalidActionException.class);
        }
    }

    @Nested
    @DisplayName("Trick evaluation")
    class TrickEvaluation {
        @Test @DisplayName("should team A win trick with strongest card")
        void shouldTeamAWinTrick() {
            Trick partial = Trick.empty()
                    .addEntry(new TrickEntry(P2, QUEEN_H))
                    .addEntry(new TrickEntry(P3, ACE_H))
                    .addEntry(new TrickEntry(P4, NINE_C));
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(SEVEN_H), P2, Hand.of(TEN_H, KING_H),
                    P3, Hand.of(JACK_H, NINE_D), P4, Hand.of(KING_D, ACE_D)));
            GameState state = new GameState(GAME_ID, GamePhase.PLAYING_TRICK, ALL_PLAYERS,
                    P1, P1, hands, List.of(), List.of(), partial, null,
                    Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()), 1, false, null, Set.of());

            GameState after = engine.apply(state, new PlayCardAction(P1, SEVEN_H));
            then(after.completedTricks()).hasSize(1);
            then(after.completedTricks().get(0).winner(ALL_PLAYERS)).contains(Team.TEAM_A);
        }

        @Test @DisplayName("should be pourri when same highest card")
        void shouldBePourri() {
            Trick partial = Trick.empty()
                    .addEntry(new TrickEntry(P1, SEVEN_H))
                    .addEntry(new TrickEntry(P2, SEVEN_D))
                    .addEntry(new TrickEntry(P3, NINE_H));
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(KING_H, ACE_H), P2, Hand.of(EIGHT_H, JACK_H),
                    P3, Hand.of(TEN_H, EIGHT_D), P4, Hand.of(NINE_D, KING_D, QUEEN_D)));
            GameState state = new GameState(GAME_ID, GamePhase.PLAYING_TRICK, ALL_PLAYERS,
                    P1, P4, hands, List.of(), List.of(), partial, null,
                    Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()), 1, false, null, Set.of());

            GameState after = engine.apply(state, new PlayCardAction(P4, NINE_D));
            then(after.completedTricks()).hasSize(1);
            then(after.completedTricks().get(0).winner(ALL_PLAYERS)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Early round termination")
    class EarlyRoundTermination {

        private Trick trickWonByA() {
            // TEAM_A wins: SEVEN_H (rank 1) beats KING_H (rank 4)
            return Trick.empty()
                    .addEntry(new TrickEntry(P1, SEVEN_H))
                    .addEntry(new TrickEntry(P2, KING_H))
                    .addEntry(new TrickEntry(P3, ACE_H))
                    .addEntry(new TrickEntry(P4, KING_D));
        }

        private Trick pourriTrick() {
            // Pourri: both teams play SEVEN (rank 1)
            return Trick.empty()
                    .addEntry(new TrickEntry(P1, SEVEN_H))
                    .addEntry(new TrickEntry(P2, SEVEN_D))
                    .addEntry(new TrickEntry(P3, ACE_H))
                    .addEntry(new TrickEntry(P4, QUEEN_D));
        }

        /** Build a state with one already-completed trick; P1 leads trick 2. */
        private GameState stateAfterOneTrick(Trick completedTrick, Map<PlayerId, Hand> hands) {
            return new GameState(GAME_ID, GamePhase.PLAYING_TRICK, ALL_PLAYERS,
                    P3, P1, hands, List.of(), List.of(completedTrick), Trick.empty(), null,
                    Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
                    1, false, null, Set.of());
        }

        /** Play all 4 cards of trick 2, P1 leading. */
        private GameState playSecondTrick(GameState state,
                                          Card p1Card, Card p2Card, Card p3Card, Card p4Card) {
            state = engine.apply(state, new PlayCardAction(P1, p1Card));
            state = engine.apply(state, new PlayCardAction(P2, p2Card));
            state = engine.apply(state, new PlayCardAction(P3, p3Card));
            return engine.apply(state, new PlayCardAction(P4, p4Card));
        }

        @Test
        @DisplayName("should end round after trick 2 when same team wins both tricks")
        void shouldEndRoundEarlyWhenSameTeamWinsBothTricks() {
            // TEAM_A wins trick 1; trick 2: TEAM_A wins again (EIGHT vs NINE)
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(EIGHT_H), P2, Hand.of(NINE_H),
                    P3, Hand.of(EIGHT_D), P4, Hand.of(NINE_D)));

            GameState result = playSecondTrick(
                    stateAfterOneTrick(trickWonByA(), hands),
                    EIGHT_H, NINE_H, EIGHT_D, NINE_D);

            then(result.phase()).isEqualTo(GamePhase.END_OF_ROUND);
            then(result.completedTricks()).hasSize(2);
        }

        @Test
        @DisplayName("should end round after trick 2 when first trick was pourri and second has a winner")
        void shouldEndRoundEarlyWhenFirstPourriSecondHasWinner() {
            // Trick 1 is pourri → P1 leads trick 2
            // Trick 2: TEAM_B wins (EIGHT vs NINE)
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(NINE_C), P2, Hand.of(EIGHT_H),
                    P3, Hand.of(NINE_D), P4, Hand.of(EIGHT_D)));

            GameState result = playSecondTrick(
                    stateAfterOneTrick(pourriTrick(), hands),
                    NINE_C, EIGHT_H, NINE_D, EIGHT_D);

            then(result.phase()).isEqualTo(GamePhase.END_OF_ROUND);
            then(result.completedTricks()).hasSize(2);
        }

        @Test
        @DisplayName("should continue to trick 3 when score is 1-1 after trick 2")
        void shouldContinueWhenSplitAfterTwoTricks() {
            // TEAM_A wins trick 1; trick 2 TEAM_B wins (EIGHT vs NINE) → 1-1
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(NINE_H), P2, Hand.of(EIGHT_H),
                    P3, Hand.of(NINE_D), P4, Hand.of(EIGHT_D)));

            GameState result = playSecondTrick(
                    stateAfterOneTrick(trickWonByA(), hands),
                    NINE_H, EIGHT_H, NINE_D, EIGHT_D);

            then(result.phase()).isEqualTo(GamePhase.PLAYING_TRICK);
            then(result.completedTricks()).hasSize(2);
        }

        @Test
        @DisplayName("should continue to trick 3 when trick 2 is pourri after a won trick 1")
        void shouldContinueWhenSecondTrickIsPourri() {
            // TEAM_A wins trick 1; trick 2 is pourri (EIGHT vs EIGHT) → 3rd trick needed
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(EIGHT_H), P2, Hand.of(EIGHT_D),
                    P3, Hand.of(NINE_D), P4, Hand.of(NINE_C)));

            GameState result = playSecondTrick(
                    stateAfterOneTrick(trickWonByA(), hands),
                    EIGHT_H, EIGHT_D, NINE_D, NINE_C);

            then(result.phase()).isEqualTo(GamePhase.PLAYING_TRICK);
            then(result.completedTricks()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Trut challenge")
    class TrutChallengeTests {

        @Test @DisplayName("should enter trut challenge phase")
        void shouldEnterTrutChallengePhase() {
            GameState after = engine.apply(createPlayingState(defaultHands()), new TrutAction(P2));
            then(after.phase()).isEqualTo(GamePhase.TRUT_CHALLENGE);
            then(after.trutChallenge().challengerTeam()).isEqualTo(Team.TEAM_B);
        }

        @Test @DisplayName("should accept trut when first responder calls")
        void shouldAcceptTrut() {
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P2));
            GameState after = engine.apply(withTrut, new CallAction(P3));
            then(after.phase()).isEqualTo(GamePhase.PLAYING_TRICK);
            then(after.trutChallenge().accepted()).isTrue();
        }

        @Test @DisplayName("should end round with petit when both opponents fold")
        void shouldEndRoundWithPetitWhenBothFold() {
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P2));
            GameState afterP3 = engine.apply(withTrut, new FoldAction(P3));
            GameState afterP1 = engine.apply(afterP3, new FoldAction(P1));
            then(afterP1.score().get(Team.TEAM_B).petits()).isEqualTo(1);
        }

        @Test @DisplayName("should reject trut when challenge already active")
        void shouldRejectDoubleTrut() {
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P2));
            thenThrownBy(() -> engine.apply(withTrut, new TrutAction(P4)))
                    .isInstanceOf(InvalidActionException.class);
        }

        @Test @DisplayName("should reject response from wrong player")
        void shouldRejectWrongPlayer() {
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P2));
            thenThrownBy(() -> engine.apply(withTrut, new CallAction(P4)))
                    .isInstanceOf(InvalidActionException.class);
        }

        @Test @DisplayName("should handle one fold and one call as accepted")
        void shouldHandleOneFoldOneCallAsAccepted() {
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P2));
            GameState afterFold = engine.apply(withTrut, new FoldAction(P3));
            GameState afterCall = engine.apply(afterFold, new CallAction(P1));
            then(afterCall.phase()).isEqualTo(GamePhase.PLAYING_TRICK);
            then(afterCall.trutChallenge().accepted()).isTrue();
        }

        @Test @DisplayName("should assign first responder to player left of challenger, not left of dealer")
        void shouldAssignFirstResponderToPlayerLeftOfChallenger() {
            // Seating: P1(A)-P2(B)-P3(A)-P4(B). Dealer=P1.
            // When P4 (TEAM_B) truts: left of P4 = P1 (TEAM_A). P1 should respond first.
            // Old behaviour (left of dealer P1 = P2): P3 would have been first.
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P4));
            then(withTrut.currentPlayerId()).isEqualTo(P1);
        }

        @Test @DisplayName("second responder should follow clockwise from challenger after first responds")
        void shouldAdvanceClockwiseForSecondResponder() {
            // P4 (TEAM_B) truts. Left of P4 = P1 (TEAM_A). P1 folds → next TEAM_A = P3.
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P4));
            GameState afterP1Fold = engine.apply(withTrut, new FoldAction(P1));
            then(afterP1Fold.currentPlayerId()).isEqualTo(P3);
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {
        @Test @DisplayName("should award petit when round won without trut")
        void shouldAwardPetitWhenNoTrut() {
            GameState s = playFullRound(false);
            // One team should have 1 petit
            int totalPetits = s.score().get(Team.TEAM_A).petits() + s.score().get(Team.TEAM_B).petits();
            then(totalPetits).isGreaterThanOrEqualTo(1);
        }

        @Test @DisplayName("should award grand when trut accepted and won")
        void shouldAwardGrandWhenTrutAccepted() {
            GameState s = playFullRound(true);
            int totalGrands = s.score().get(Team.TEAM_A).grands() + s.score().get(Team.TEAM_B).grands();
            then(totalGrands).isGreaterThanOrEqualTo(1);
        }

        private GameState playFullRound(boolean withTrut) {
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(SEVEN_H, NINE_H, ACE_H),
                    P2, Hand.of(QUEEN_H, SEVEN_D, NINE_D),
                    P3, Hand.of(KING_H, NINE_C, KING_D),
                    P4, Hand.of(JACK_H, EIGHT_H, TEN_H)));
            GameState state = new GameState(GAME_ID, GamePhase.PLAYING_TRICK, ALL_PLAYERS,
                    P1, P2, hands, List.of(), List.of(), Trick.empty(), null,
                    Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()), 1, false, null, Set.of());

            if (withTrut) {
                state = engine.apply(state, new TrutAction(P2));
                state = engine.apply(state, new CallAction(P3));
            }
            while (state.phase() == GamePhase.PLAYING_TRICK) {
                PlayerId cp = state.currentPlayerId();
                Hand h = state.hands().get(cp);
                if (h == null || h.size() == 0) break;
                state = engine.apply(state, new PlayCardAction(cp, h.cards().get(0)));
            }
            return state;
        }
    }

    @Nested
    @DisplayName("Brelan and Deux Pareilles")
    class BrelanTests {
        @Test @DisplayName("should auto-trut on brelan")
        void shouldAutoTrutOnBrelan() {
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(SEVEN_H, KING_H, NINE_H),
                    P2, Hand.of(NINE_D, NINE_C, NINE_S),
                    P3, Hand.of(ACE_H, JACK_H, TEN_H),
                    P4, Hand.of(ACE_D, KING_D, QUEEN_D)));
            GameState after = engine.apply(createPlayingState(hands), new BrellanAction(P2));
            then(after.phase()).isEqualTo(GamePhase.TRUT_CHALLENGE);
        }

        @Test @DisplayName("should auto-trut on deux pareilles")
        void shouldAutoTrutOnDeuxPareilles() {
            Map<PlayerId, Hand> hands = new HashMap<>(Map.of(
                    P1, Hand.of(SEVEN_H, KING_H, NINE_H),
                    P2, Hand.of(ACE_H, ACE_D, NINE_D),
                    P3, Hand.of(EIGHT_H, JACK_H, TEN_H),
                    P4, Hand.of(KING_D, QUEEN_D, QUEEN_H)));
            GameState after = engine.apply(createPlayingState(hands), new DeuxPareillesAction(P2));
            then(after.phase()).isEqualTo(GamePhase.TRUT_CHALLENGE);
        }

        @Test @DisplayName("should reject brelan without brelan in hand")
        void shouldRejectBrelanWithoutBrelan() {
            thenThrownBy(() -> engine.apply(createPlayingState(defaultHands()), new BrellanAction(P2)))
                    .isInstanceOf(InvalidActionException.class);
        }
    }

    @Nested
    @DisplayName("Available actions")
    class AvailableActionsTests {
        @Test @DisplayName("should offer play card and trut during trick")
        void shouldOfferPlayCardAndTrut() {
            List<GameAction> actions = engine.availableActions(createPlayingState(defaultHands()), P2);
            then(actions.stream().anyMatch(a -> a instanceof PlayCardAction)).isTrue();
            then(actions.stream().anyMatch(a -> a instanceof TrutAction)).isTrue();
        }

        @Test @DisplayName("should offer call and fold during trut challenge")
        void shouldOfferCallAndFold() {
            GameState withTrut = engine.apply(createPlayingState(defaultHands()), new TrutAction(P2));
            List<GameAction> actions = engine.availableActions(withTrut, P3);
            then(actions.stream().anyMatch(a -> a instanceof CallAction)).isTrue();
            then(actions.stream().anyMatch(a -> a instanceof FoldAction)).isTrue();
        }

        @Test @DisplayName("should offer no actions when game over")
        void shouldOfferNoActionWhenGameOver() {
            GameState state = new GameState(GAME_ID, GamePhase.GAME_OVER, ALL_PLAYERS,
                    P1, null, Map.of(P1, Hand.empty(), P2, Hand.empty(), P3, Hand.empty(), P4, Hand.empty()),
                    List.of(), List.of(), Trick.empty(), null,
                    Map.of(Team.TEAM_A, new TokenCount(7, 0), Team.TEAM_B, TokenCount.zero()),
                    10, false, Team.TEAM_A, Set.of());
            then(engine.availableActions(state, P1)).isEmpty();
        }
    }

    @Nested
    @DisplayName("createInitialState")
    class CreateInitialStateTests {
        @Test @DisplayName("should set phase to WAITING_FOR_PLAYERS")
        void shouldSetPhase() {
            then(TrutGameEngine.createInitialState(GAME_ID, ALL_PLAYERS).phase())
                    .isEqualTo(GamePhase.WAITING_FOR_PLAYERS);
        }

        @Test @DisplayName("should initialize score to zero")
        void shouldInitializeScore() {
            GameState s = TrutGameEngine.createInitialState(GAME_ID, ALL_PLAYERS);
            then(s.score().get(Team.TEAM_A)).isEqualTo(TokenCount.zero());
            then(s.score().get(Team.TEAM_B)).isEqualTo(TokenCount.zero());
        }
    }

    @Nested
    @DisplayName("Full game simulation")
    class FullGameSimulation {
        @Test @DisplayName("should complete full game with winner")
        void shouldCompleteFullGameWithWinner() {
            GameState state = engine.startNewRound(TrutGameEngine.createInitialState(GAME_ID, ALL_PLAYERS));
            int maxIter = 5000;
            for (int i = 0; i < maxIter && state.phase() != GamePhase.GAME_OVER; i++) {
                state = switch (state.phase()) {
                    case PLAYING_TRICK -> {
                        PlayerId cp = state.currentPlayerId();
                        Hand h = state.hands().get(cp);
                        yield engine.apply(state, new PlayCardAction(cp, h.cards().get(0)));
                    }
                    case TRUT_CHALLENGE -> {
                        // During fortial, folding is forbidden — must call
                        PlayerId resp = state.currentPlayerId();
                        yield state.fortialActive()
                            ? engine.apply(state, new CallAction(resp))
                            : engine.apply(state, new FoldAction(resp));
                    }
                    case END_OF_ROUND -> engine.startNewRound(state);
                    case FORTIAL_DECISION -> engine.apply(state, new TrutAction(state.currentPlayerId()));
                    default -> state;
                };
            }
            then(state.phase()).isEqualTo(GamePhase.GAME_OVER);
            then(state.winner()).isNotNull();
            then(state.score().get(state.winner()).hasWon()).isTrue();
        }
    }
}
