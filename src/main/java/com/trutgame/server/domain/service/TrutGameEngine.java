package com.trutgame.server.domain.service;

import com.trutgame.server.domain.action.*;
import com.trutgame.server.domain.exception.InvalidActionException;
import com.trutgame.server.domain.model.*;
import com.trutgame.server.domain.phase.GamePhase;

import java.util.*;

/**
 * Core game engine implementing the complete Trut card-game rules.
 * <p>
 * Pure Java — no framework dependencies.
 * {@link GameState} is immutable: every method returns a <em>new</em> GameState.
 */
public class TrutGameEngine {

    private static final int PLAYER_COUNT = 4;
    private static final int CARDS_PER_PLAYER = 3;
    private static final int TRICKS_PER_ROUND = 3;

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Apply a player action to the current state.
     *
     * @return a new, updated GameState
     * @throws InvalidActionException if the action violates the rules
     */
    public GameState apply(GameState state, GameAction action) throws InvalidActionException {
        return switch (action) {
            case PlayCardAction a      -> applyPlayCard(state, a);
            case TrutAction a          -> applyTrut(state, a);
            case CallAction a          -> applyCall(state, a);
            case FoldAction a          -> applyFold(state, a);
            case BrellanAction a       -> applyBrellan(state, a);
            case DeuxPareillesAction a -> applyDeuxPareilles(state, a);
        };
    }

    /**
     * Compute the list of legal actions a given player can perform right now.
     */
    public List<GameAction> availableActions(GameState state, PlayerId playerId) {
        if (state.phase() == GamePhase.GAME_OVER
                || state.phase() == GamePhase.WAITING_FOR_PLAYERS
                || state.phase() == GamePhase.DEALING
                || state.phase() == GamePhase.END_OF_ROUND) {
            return List.of();
        }

        List<GameAction> actions = new ArrayList<>();

        switch (state.phase()) {
            case PLAYING_TRICK -> {
                // Current player may play any card in hand
                if (state.currentPlayerId() != null
                        && state.currentPlayerId().equals(playerId)) {
                    Hand hand = state.hands().get(playerId);
                    if (hand != null) {
                        for (Card card : hand.cards()) {
                            actions.add(new PlayCardAction(playerId, card));
                        }
                    }
                }

                // Any player may trut / brellan / deux-pareilles when no challenge is active
                if (state.trutChallenge() == null) {
                    Hand hand = state.hands().get(playerId);
                    if (hand != null && hand.size() > 0) {
                        actions.add(new TrutAction(playerId));
                        if (hand.hasBrelan()) {
                            actions.add(new BrellanAction(playerId));
                        }
                        if (hand.hasDeuxPareillesUneFausse()) {
                            actions.add(new DeuxPareillesAction(playerId));
                        }
                    }
                }
            }

            case TRUT_CHALLENGE -> {
                // Only the current responder may act
                if (state.currentPlayerId() != null
                        && state.currentPlayerId().equals(playerId)) {
                    actions.add(new CallAction(playerId));
                    if (!state.fortialActive()) {
                        actions.add(new FoldAction(playerId));
                    }
                }
            }

            case FORTIAL_DECISION -> {
                // Only the current fortial-team player may act
                if (state.currentPlayerId() != null
                        && state.currentPlayerId().equals(playerId)) {
                    actions.add(new TrutAction(playerId));
                    actions.add(new FoldAction(playerId)); // pass
                }
            }

            default -> { /* no actions */ }
        }

        return List.copyOf(actions);
    }

    /**
     * Deal cards and transition from END_OF_ROUND (or WAITING_FOR_PLAYERS)
     * into the next round.  Handles dealer rotation, fortial detection
     * and initial phase selection.
     */
    public GameState startNewRound(GameState state) {
        int newRoundNumber = state.roundNumber() + 1;

        // On the very first round, ensure players alternate teams around the table (A-B-A-B)
        List<Player> players = state.players();
        if (state.phase() == GamePhase.WAITING_FOR_PLAYERS || state.roundNumber() == 0) {
            players = ensureAlternatingTeams(players);
        }

        // Rotate dealer — keep the same for the very first round
        PlayerId dealerId;
        if (state.currentDealerId() == null) {
            dealerId = players.get(0).id();
        } else if (state.roundNumber() == 0) {
            dealerId = state.currentDealerId();
        } else {
            dealerId = nextPlayerFrom(players, state.currentDealerId());
        }

        // Build deal order: clockwise starting from left of new dealer
        PlayerId leftOfDealer = nextPlayerFrom(players, dealerId);
        List<PlayerId> dealOrder = buildClockwiseOrder(players, leftOfDealer);

        // Shuffle and deal one card at a time, 3 rounds
        List<Card> deck = buildDeck();
        Collections.shuffle(deck);

        Map<PlayerId, List<Card>> cardLists = new LinkedHashMap<>();
        for (PlayerId pid : dealOrder) {
            cardLists.put(pid, new ArrayList<>());
        }
        int idx = 0;
        for (int round = 0; round < CARDS_PER_PLAYER; round++) {
            for (PlayerId pid : dealOrder) {
                cardLists.get(pid).add(deck.get(idx++));
            }
        }

        Map<PlayerId, Hand> hands = new HashMap<>();
        for (var entry : cardLists.entrySet()) {
            hands.put(entry.getKey(), new Hand(entry.getValue()));
        }
        List<Card> talon = new ArrayList<>(deck.subList(idx, deck.size()));

        // ── Detect fortial ──────────────────────────────────────────────────
        Map<Team, TokenCount> score = state.score();
        boolean fortialA = score.get(Team.TEAM_A).isFortial();
        boolean fortialB = score.get(Team.TEAM_B).isFortial();
        boolean bothFortial = fortialA && fortialB;
        boolean anyFortial  = fortialA || fortialB;

        GamePhase phase;
        PlayerId firstPlayer;
        boolean fortialActive;

        if (bothFortial) {
            // Both fortial → play normally; round winner wins the game
            phase = GamePhase.PLAYING_TRICK;
            firstPlayer = leftOfDealer;
            fortialActive = true;
        } else if (anyFortial) {
            Team fortialTeam = fortialA ? Team.TEAM_A : Team.TEAM_B;
            phase = GamePhase.FORTIAL_DECISION;
            firstPlayer = firstPlayerOfTeamClockwise(
                    players, dealerId, fortialTeam);
            fortialActive = true;
        } else {
            phase = GamePhase.PLAYING_TRICK;
            firstPlayer = leftOfDealer;
            fortialActive = false;
        }

        return new GameState(
                state.gameId(), phase, players, dealerId,
                firstPlayer, hands, talon, List.of(),
                Trick.empty(), null, score, newRoundNumber,
                fortialActive, null, Set.of()
        );
    }

    /**
     * Create the very first GameState before any round is played.
     */
    public static GameState createInitialState(String gameId,
                                               List<Player> players,
                                               PlayerId firstDealerId) {
        return new GameState(
                gameId, GamePhase.WAITING_FOR_PLAYERS, players,
                firstDealerId, null,
                Map.of(), List.of(), List.of(), Trick.empty(), null,
                Map.of(Team.TEAM_A, TokenCount.zero(),
                       Team.TEAM_B, TokenCount.zero()),
                0, false, null, Set.of()
        );
    }

    /** Convenience overload — first player becomes the first dealer. */
    public static GameState createInitialState(String gameId, List<Player> players) {
        return createInitialState(gameId, players, players.get(0).id());
    }

    /**
     * Build a standard 32-card deck ({@link CardValue} × {@link Suit}).
     */
    public static List<Card> buildDeck() {
        List<Card> deck = new ArrayList<>(32);
        for (Suit suit : Suit.values()) {
            for (CardValue value : CardValue.values()) {
                deck.add(new Card(value, suit));
            }
        }
        return deck;
    }

    // ── Play Card ───────────────────────────────────────────────────────────

    private GameState applyPlayCard(GameState state, PlayCardAction action) {
        requirePhase(state, GamePhase.PLAYING_TRICK);
        requireCurrentPlayer(state, action.playerId());

        Hand hand = state.hands().get(action.playerId());
        if (hand == null || !hand.contains(action.card())) {
            throw new InvalidActionException(
                    "Card not in player's hand: " + action.card().id());
        }

        // Remove card from hand
        Map<PlayerId, Hand> newHands = new HashMap<>(state.hands());
        newHands.put(action.playerId(), hand.remove(action.card()));

        // Add entry to current trick
        Trick updatedTrick = state.currentTrick()
                .addEntry(new TrickEntry(action.playerId(), action.card()));

        GameState afterPlay = copy(state)
                .hands(newHands)
                .currentTrick(updatedTrick)
                .build();

        // Trick complete?
        if (updatedTrick.isComplete(PLAYER_COUNT)) {
            return evaluateCompletedTrick(afterPlay);
        }

        // Advance to next player
        PlayerId nextPlayer = state.nextPlayer(action.playerId()).id();
        return copy(afterPlay).currentPlayerId(nextPlayer).build();
    }

    private GameState evaluateCompletedTrick(GameState state) {
        Trick completedTrick = state.currentTrick();
        List<Trick> newCompleted = new ArrayList<>(state.completedTricks());
        newCompleted.add(completedTrick);

        // All tricks played → resolve round
        if (newCompleted.size() >= TRICKS_PER_ROUND) {
            return resolveEndOfRound(
                    copy(state)
                            .completedTricks(newCompleted)
                            .currentTrick(Trick.empty())
                            .build());
        }

        // Round decided after 2 tricks → skip the 3rd trick
        if (newCompleted.size() == TRICKS_PER_ROUND - 1
                && isRoundDecidedAfterTwoTricks(newCompleted, state.players())) {
            return resolveEndOfRound(
                    copy(state)
                            .completedTricks(newCompleted)
                            .currentTrick(Trick.empty())
                            .build());
        }

        // Determine next trick leader
        Optional<Team> winner = completedTrick.winner(state.players());
        PlayerId nextLeader = winner.isPresent()
                ? findTrickLeader(completedTrick, state.players(), winner.get())
                : findPourrisseur(completedTrick);

        return copy(state)
                .completedTricks(newCompleted)
                .currentTrick(Trick.empty())
                .currentPlayerId(nextLeader)
                .build();
    }

    /**
     * Returns true when the round winner is already determined after exactly 2 tricks,
     * making the 3rd trick unnecessary.
     * <ul>
     *   <li>Both tricks won by the same team (2-0).</li>
     *   <li>First trick was pourri, second trick has a winner (winner accumulates 2 tricks).</li>
     * </ul>
     * NOT decided: 1-1 split, first-won + second-pourri, or both pourri.
     */
    private boolean isRoundDecidedAfterTwoTricks(List<Trick> tricks, List<Player> players) {
        Optional<Team> first = tricks.get(0).winner(players);
        Optional<Team> second = tricks.get(1).winner(players);
        // Both won by same team
        if (first.isPresent() && second.isPresent() && first.get() == second.get()) return true;
        // First was pourri, second has a winner → winner accumulates 2 tricks (including the pourri)
        return first.isEmpty() && second.isPresent();
    }

    // ── End of Round ────────────────────────────────────────────────────────

    /**
     * Resolve pourri distribution, compute the round winner and update scores.
     * Returns a state in {@code END_OF_ROUND} or {@code GAME_OVER}.
     */
    private GameState resolveEndOfRound(GameState state) {
        Map<Team, Integer> trickWins = resolveTrickWinners(
                state.completedTricks(), state.players());

        int winsA = trickWins.getOrDefault(Team.TEAM_A, 0);
        int winsB = trickWins.getOrDefault(Team.TEAM_B, 0);

        // All tricks pourri → null round
        if (winsA == 0 && winsB == 0) {
            return copy(state).phase(GamePhase.END_OF_ROUND).build();
        }

        // Tie (shouldn't happen with 3 tricks, but guard anyway)
        if (winsA == winsB) {
            return copy(state).phase(GamePhase.END_OF_ROUND).build();
        }

        Team roundWinner = winsA > winsB ? Team.TEAM_A : Team.TEAM_B;

        // Both-fortial: round winner wins the game outright
        if (state.fortialActive() && isBothFortial(state.score())) {
            return copy(state)
                    .phase(GamePhase.GAME_OVER)
                    .winner(roundWinner)
                    .build();
        }

        boolean trutAccepted = state.trutChallenge() != null
                && state.trutChallenge().accepted();
        Map<Team, TokenCount> newScore =
                updateScore(state.score(), roundWinner, trutAccepted);

        if (newScore.get(roundWinner).hasWon()) {
            return copy(state)
                    .phase(GamePhase.GAME_OVER)
                    .score(newScore)
                    .winner(roundWinner)
                    .build();
        }

        return copy(state)
                .phase(GamePhase.END_OF_ROUND)
                .score(newScore)
                .build();
    }

    /**
     * Walk through the completed tricks and distribute pourri tricks.
     * <ul>
     *   <li>A pourri trick is set aside and awarded to the winner of the next won trick.</li>
     *   <li>Trailing pourris are awarded to the winner of the first won trick.</li>
     *   <li>If all tricks are pourri → both entries stay 0 (null round).</li>
     * </ul>
     */
    private Map<Team, Integer> resolveTrickWinners(List<Trick> tricks,
                                                   List<Player> players) {
        Map<Team, Integer> wins = new EnumMap<>(Team.class);
        wins.put(Team.TEAM_A, 0);
        wins.put(Team.TEAM_B, 0);

        int pendingPourri = 0;
        Team firstWinner = null;

        for (Trick trick : tricks) {
            Optional<Team> winner = trick.winner(players);
            if (winner.isPresent()) {
                Team team = winner.get();
                wins.merge(team, 1 + pendingPourri, Integer::sum);
                pendingPourri = 0;
                if (firstWinner == null) {
                    firstWinner = team;
                }
            } else {
                pendingPourri++;
            }
        }

        // Trailing pourris go to the first trick winner
        if (pendingPourri > 0 && firstWinner != null) {
            wins.merge(firstWinner, pendingPourri, Integer::sum);
        }

        return wins;
    }

    // ── Trut ────────────────────────────────────────────────────────────────

    private GameState applyTrut(GameState state, TrutAction action) {
        return applyTrutWithType(state, action, TrutChallenge.ChallengeType.TRUT);
    }

    private GameState applyTrutWithType(GameState state, TrutAction action, TrutChallenge.ChallengeType challengeType) {
        if (state.phase() == GamePhase.FORTIAL_DECISION) {
            return applyFortialTrut(state, action);
        }

        requirePhase(state, GamePhase.PLAYING_TRICK);
        if (state.trutChallenge() != null) {
            throw new InvalidActionException("A trut challenge is already active");
        }

        Team challengerTeam = state.getTeam(action.playerId());
        TrutChallenge challenge = TrutChallenge.create(action.playerId(), challengerTeam, challengeType);
        // First responder = first opponent encountered clockwise from the challenger's left seat
        PlayerId firstResponder = findNextResponder(state.players(),
                action.playerId(), challenge);

        return copy(state)
                .phase(GamePhase.TRUT_CHALLENGE)
                .trutChallenge(challenge)
                .currentPlayerId(firstResponder)
                .build();
    }

    /**
     * Fortial trut: opponents are forced to call → auto-accept.
     */
    private GameState applyFortialTrut(GameState state, TrutAction action) {
        requireCurrentPlayer(state, action.playerId());
        if (state.trutChallenge() != null) {
            throw new InvalidActionException("A trut challenge is already active");
        }

        Team challengerTeam = state.getTeam(action.playerId());
        Team opposingTeam = challengerTeam.opponent();

        // Auto-accept: both opponents are forced to call
        TrutChallenge challenge = TrutChallenge.create(action.playerId(), challengerTeam, TrutChallenge.ChallengeType.TRUT);
        for (Player opp : state.teamPlayers(opposingTeam)) {
            challenge = challenge.addResponse(opp.id(), true);
        }
        challenge = challenge.resolve();

        PlayerId trickLeader = computeNextTrickPlayer(state);
        return copy(state)
                .phase(GamePhase.PLAYING_TRICK)
                .trutChallenge(challenge)
                .currentPlayerId(trickLeader)
                .build();
    }

    // ── Call (aller voir) ───────────────────────────────────────────────────

    private GameState applyCall(GameState state, CallAction action) {
        requirePhase(state, GamePhase.TRUT_CHALLENGE);
        requireCurrentPlayer(state, action.playerId());
        validateResponder(state, action.playerId());

        // At least one opponent called → challenge accepted immediately
        TrutChallenge resolved = state.trutChallenge()
                .addResponse(action.playerId(), true)
                .resolve();

        PlayerId nextTrickPlayer = computeNextTrickPlayer(state);

        return copy(state)
                .phase(GamePhase.PLAYING_TRICK)
                .trutChallenge(resolved)
                .currentPlayerId(nextTrickPlayer)
                .build();
    }

    // ── Fold (se coucher) ───────────────────────────────────────────────────

    private GameState applyFold(GameState state, FoldAction action) {
        if (state.phase() == GamePhase.FORTIAL_DECISION) {
            return applyFortialFold(state, action);
        }

        requirePhase(state, GamePhase.TRUT_CHALLENGE);
        requireCurrentPlayer(state, action.playerId());
        validateResponder(state, action.playerId());

        if (state.fortialActive()) {
            throw new InvalidActionException(
                    "Cannot fold during fortial — se coucher = défaite automatique");
        }

        TrutChallenge updated = state.trutChallenge()
                .addResponse(action.playerId(), false);

        // Check whether all opponents have now responded
        Team opposingTeam = updated.challengerTeam().opponent();
        boolean allResponded = state.teamPlayers(opposingTeam).stream()
                .allMatch(p -> updated.respondedPlayers().contains(p.id()));

        if (allResponded) {
            TrutChallenge resolved = updated.resolve();

            if (resolved.accepted()) {
                // One called, one folded → accepted → resume play
                PlayerId nextTrickPlayer = computeNextTrickPlayer(state);
                return copy(state)
                        .phase(GamePhase.PLAYING_TRICK)
                        .trutChallenge(resolved)
                        .currentPlayerId(nextTrickPlayer)
                        .build();
            }

            // Both folded → trutor team gets 1 petit, round ends
            return applyFoldScoring(state, resolved);
        }

        // Need next responder — walk clockwise from challenger's left
        PlayerId nextResponder = findNextResponder(state.players(),
                state.trutChallenge().challengerId(), updated);
        return copy(state)
                .trutChallenge(updated)
                .currentPlayerId(nextResponder)
                .build();
    }

    /**
     * Both opponents folded: trutor team gets 1 petit.
     */
    private GameState applyFoldScoring(GameState state, TrutChallenge resolved) {
        Team trutorTeam = resolved.challengerTeam();
        Map<Team, TokenCount> newScore =
                updateScore(state.score(), trutorTeam, false);

        GamePhase endPhase = newScore.get(trutorTeam).hasWon()
                ? GamePhase.GAME_OVER
                : GamePhase.END_OF_ROUND;
        Team winner = endPhase == GamePhase.GAME_OVER ? trutorTeam : null;

        return copy(state)
                .phase(endPhase)
                .trutChallenge(resolved)
                .score(newScore)
                .winner(winner)
                .build();
    }

    // ── Fortial fold (pass) ─────────────────────────────────────────────────

    /**
     * During {@code FORTIAL_DECISION}, a fold means "I pass".
     * <ul>
     *   <li>First fortial player passes → partner decides.</li>
     *   <li>Partner also passes → opponent gets 1 petit, round ends.</li>
     * </ul>
     */
    private GameState applyFortialFold(GameState state, FoldAction action) {
        requireCurrentPlayer(state, action.playerId());

        Team fortialTeam = state.getTeam(action.playerId());
        PlayerId firstFortial = firstPlayerOfTeamClockwise(
                state.players(), state.currentDealerId(), fortialTeam);

        if (state.currentPlayerId().equals(firstFortial)) {
            // First player passes → advance to partner
            PlayerId partnerId = state.teamPlayers(fortialTeam).stream()
                    .map(Player::id)
                    .filter(id -> !id.equals(action.playerId()))
                    .findFirst()
                    .orElseThrow();
            return copy(state).currentPlayerId(partnerId).build();
        }

        // Second player also passes → opponent gets 1 petit
        Team opponentTeam = fortialTeam.opponent();
        Map<Team, TokenCount> newScore =
                updateScore(state.score(), opponentTeam, false);

        GamePhase endPhase = newScore.get(opponentTeam).hasWon()
                ? GamePhase.GAME_OVER
                : GamePhase.END_OF_ROUND;
        Team winner = endPhase == GamePhase.GAME_OVER ? opponentTeam : null;

        return copy(state)
                .phase(endPhase)
                .score(newScore)
                .winner(winner)
                .build();
    }

    // ── Brellan & Deux Pareilles (auto-trut) ────────────────────────────────

    private GameState applyBrellan(GameState state, BrellanAction action) {
        requirePhase(state, GamePhase.PLAYING_TRICK);
        if (state.trutChallenge() != null) {
            throw new InvalidActionException("A trut challenge is already active");
        }
        Hand hand = state.hands().get(action.playerId());
        if (hand == null || !hand.hasBrelan()) {
            throw new InvalidActionException("Player does not have a brelan");
        }
        return applyTrutWithType(state, new TrutAction(action.playerId()), TrutChallenge.ChallengeType.BRELLAN);
    }

    private GameState applyDeuxPareilles(GameState state, DeuxPareillesAction action) {
        requirePhase(state, GamePhase.PLAYING_TRICK);
        if (state.trutChallenge() != null) {
            throw new InvalidActionException("A trut challenge is already active");
        }
        Hand hand = state.hands().get(action.playerId());
        if (hand == null || !hand.hasDeuxPareillesUneFausse()) {
            throw new InvalidActionException("Player does not have deux pareilles");
        }
        return applyTrutWithType(state, new TrutAction(action.playerId()), TrutChallenge.ChallengeType.DEUX_PAREILLES);
    }

    // ── Scoring ─────────────────────────────────────────────────────────────

    /**
     * Update the score map after a round or fold.
     * <ul>
     *   <li>Trut accepted → winner gains 1 grand, opponent loses all petits.</li>
     *   <li>No trut → winner gains 1 petit; if the petit triggers the
     *       3→1 grand conversion the opponent loses all petits too.</li>
     * </ul>
     */
    private Map<Team, TokenCount> updateScore(Map<Team, TokenCount> current,
                                              Team winningTeam,
                                              boolean trutAccepted) {
        Map<Team, TokenCount> score = new EnumMap<>(Team.class);
        score.putAll(current);
        Team losingTeam = winningTeam.opponent();

        if (trutAccepted) {
            score.put(winningTeam, score.get(winningTeam).addGrand());
            score.put(losingTeam, score.get(losingTeam).losePetits());
        } else {
            TokenCount before = score.get(winningTeam);
            TokenCount after  = before.addPetit();
            score.put(winningTeam, after);
            // 3 petits → 1 grand conversion triggers opponent losing petits
            if (after.grands() > before.grands()) {
                score.put(losingTeam, score.get(losingTeam).losePetits());
            }
        }
        return score;
    }

    // ── Trick helpers ───────────────────────────────────────────────────────

    /**
     * The <em>pourrisseur</em>: the <b>last</b> player who played a card
     * equal to the trick's best value — the player who caused the tie.
     * "Qui pourrit dépourrit."
     */
    private PlayerId findPourrisseur(Trick trick) {
        int bestRank = Integer.MAX_VALUE;
        PlayerId pourrisseur = null;
        for (TrickEntry entry : trick.entries()) {
            if (entry.card().value().rank() <= bestRank) {
                bestRank = entry.card().value().rank();
                pourrisseur = entry.playerId();
            }
        }
        if (pourrisseur == null) {
            throw new IllegalStateException("Empty trick has no pourrisseur");
        }
        return pourrisseur;
    }

    /**
     * Find the player from the winning team who played the best card.
     * This player leads the next trick.
     */
    private PlayerId findTrickLeader(Trick trick, List<Player> players,
                                     Team winningTeam) {
        int bestRank = Integer.MAX_VALUE;
        PlayerId leader = null;
        for (TrickEntry entry : trick.entries()) {
            Player player = findPlayer(players, entry.playerId());
            if (player.team() == winningTeam
                    && entry.card().value().rank() < bestRank) {
                bestRank = entry.card().value().rank();
                leader = entry.playerId();
            }
        }
        if (leader == null) {
            throw new IllegalStateException(
                    "No player from winning team found in trick");
        }
        return leader;
    }

    /**
     * Compute which player should play the next card, based on
     * the current trick state and completed tricks.
     * Used when resuming play after a trut challenge.
     */
    private PlayerId computeNextTrickPlayer(GameState state) {
        Trick currentTrick = state.currentTrick();

        // Mid-trick: next player clockwise after the last card played
        if (currentTrick.size() > 0) {
            TrickEntry last = currentTrick.entries().get(currentTrick.size() - 1);
            return state.nextPlayer(last.playerId()).id();
        }

        // First trick of round
        if (state.completedTricks().isEmpty()) {
            return state.playerLeftOfDealer().id();
        }

        // Between tricks: determined by last completed trick
        Trick lastCompleted = state.completedTricks()
                .get(state.completedTricks().size() - 1);
        Optional<Team> lastWinner = lastCompleted.winner(state.players());
        return lastWinner.isPresent()
                ? findTrickLeader(lastCompleted, state.players(), lastWinner.get())
                : findPourrisseur(lastCompleted);
    }

    // ── Trut challenge helpers ──────────────────────────────────────────────

    /**
     * Find the next opponent who has not yet responded to the trut challenge,
     * walking clockwise from the seat left of the challenger.
     */
    private PlayerId findNextResponder(List<Player> players,
                                       PlayerId challengerId,
                                       TrutChallenge challenge) {
        Team opposingTeam = challenge.challengerTeam().opponent();
        PlayerId leftOfChallenger = nextPlayerFrom(players, challengerId);
        List<PlayerId> clockwise = buildClockwiseOrder(players, leftOfChallenger);
        for (PlayerId pid : clockwise) {
            Player p = findPlayer(players, pid);
            if (p.team() == opposingTeam
                    && !challenge.respondedPlayers().contains(pid)) {
                return pid;
            }
        }
        throw new IllegalStateException("No remaining responder found");
    }

    private void validateResponder(GameState state, PlayerId playerId) {
        TrutChallenge challenge = state.trutChallenge();
        if (challenge == null) {
            throw new InvalidActionException("No active trut challenge");
        }
        Team opposingTeam = challenge.challengerTeam().opponent();
        if (state.getTeam(playerId) != opposingTeam) {
            throw new InvalidActionException(
                    "Only the opposing team can respond to a trut");
        }
        if (challenge.respondedPlayers().contains(playerId)) {
            throw new InvalidActionException("Player already responded");
        }
    }

    // ── Fortial helpers ─────────────────────────────────────────────────────

    /**
     * First player of the given team in clockwise order starting from the
     * seat left of the dealer.
     */
    private PlayerId firstPlayerOfTeamClockwise(List<Player> players,
                                                PlayerId dealerId,
                                                Team team) {
        PlayerId leftOfDealer = nextPlayerFrom(players, dealerId);
        PlayerId current = leftOfDealer;
        for (int i = 0; i < players.size(); i++) {
            if (findPlayer(players, current).team() == team) {
                return current;
            }
            current = nextPlayerFrom(players, current);
        }
        throw new IllegalStateException("No player found for team " + team);
    }

    private boolean isBothFortial(Map<Team, TokenCount> score) {
        return score.get(Team.TEAM_A).isFortial()
                && score.get(Team.TEAM_B).isFortial();
    }

    // ── Seating order ─────────────────────────────────────────────────────

    /**
     * Reorder players so that teams alternate around the table: A-B-A-B.
     * Preserves the relative order within each team. Updates seatIndex accordingly.
     */
    static List<Player> ensureAlternatingTeams(List<Player> players) {
        if (players.size() != PLAYER_COUNT) return players;

        // Check if already alternating
        boolean alreadyAlternating = true;
        for (int i = 1; i < players.size(); i++) {
            if (players.get(i).team() == players.get(i - 1).team()) {
                alreadyAlternating = false;
                break;
            }
        }
        if (alreadyAlternating) return players;

        // Split by team, preserving relative order
        List<Player> teamA = new ArrayList<>();
        List<Player> teamB = new ArrayList<>();
        for (Player p : players) {
            if (p.team() == Team.TEAM_A) teamA.add(p);
            else teamB.add(p);
        }

        // Interleave: A0, B0, A1, B1
        List<Player> reordered = new ArrayList<>(PLAYER_COUNT);
        for (int i = 0; i < Math.min(teamA.size(), teamB.size()); i++) {
            reordered.add(withSeatIndex(teamA.get(i), reordered.size()));
            reordered.add(withSeatIndex(teamB.get(i), reordered.size()));
        }
        return List.copyOf(reordered);
    }

    private static Player withSeatIndex(Player p, int newSeatIndex) {
        return new Player(p.id(), p.pseudo(), p.team(), newSeatIndex, p.isAi());
    }

    // ── Player-list navigation ──────────────────────────────────────────────

    private static Player findPlayer(List<Player> players, PlayerId id) {
        for (Player p : players) {
            if (p.id().equals(id)) return p;
        }
        throw new IllegalArgumentException("Player not found: " + id);
    }

    private static PlayerId nextPlayerFrom(List<Player> players, PlayerId current) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).id().equals(current)) {
                return players.get((i + 1) % players.size()).id();
            }
        }
        throw new IllegalArgumentException("Player not found: " + current);
    }

    /**
     * Build a list of all player IDs in clockwise order starting at {@code startId}.
     */
    private static List<PlayerId> buildClockwiseOrder(List<Player> players,
                                                      PlayerId startId) {
        int startIdx = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).id().equals(startId)) {
                startIdx = i;
                break;
            }
        }
        if (startIdx == -1) {
            throw new IllegalArgumentException("Player not found: " + startId);
        }
        List<PlayerId> order = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            order.add(players.get((startIdx + i) % players.size()).id());
        }
        return order;
    }

    // ── Validation helpers ──────────────────────────────────────────────────

    private static void requirePhase(GameState state, GamePhase expected) {
        if (state.phase() != expected) {
            throw new InvalidActionException(
                    "Expected phase " + expected + " but was " + state.phase());
        }
    }

    private static void requireCurrentPlayer(GameState state, PlayerId playerId) {
        if (state.currentPlayerId() == null
                || !state.currentPlayerId().equals(playerId)) {
            throw new InvalidActionException(
                    "Not this player's turn. Expected "
                            + state.currentPlayerId() + " but got " + playerId);
        }
    }

    // ── Immutable-state copy builder ────────────────────────────────────────

    private static StateBuilder copy(GameState s) {
        return new StateBuilder(s);
    }

    /**
     * Lightweight mutable builder used exclusively inside this engine to
     * produce modified copies of the immutable {@link GameState} record.
     */
    private static final class StateBuilder {
        private final String gameId;
        private GamePhase phase;
        private List<Player> players;
        private final PlayerId currentDealerId;
        private PlayerId currentPlayerId;
        private Map<PlayerId, Hand> hands;
        private final List<Card> talon;
        private List<Trick> completedTricks;
        private Trick currentTrick;
        private TrutChallenge trutChallenge;
        private Map<Team, TokenCount> score;
        private final int roundNumber;
        private final boolean fortialActive;
        private Team winner;
        private Set<PlayerId> rematchVotes;

        StateBuilder(GameState s) {
            this.gameId          = s.gameId();
            this.phase           = s.phase();
            this.players         = s.players();
            this.currentDealerId = s.currentDealerId();
            this.currentPlayerId = s.currentPlayerId();
            this.hands           = s.hands();
            this.talon           = s.talon();
            this.completedTricks = s.completedTricks();
            this.currentTrick    = s.currentTrick();
            this.trutChallenge   = s.trutChallenge();
            this.score           = s.score();
            this.roundNumber     = s.roundNumber();
            this.fortialActive   = s.fortialActive();
            this.winner          = s.winner();
            this.rematchVotes    = s.rematchVotes();
        }

        StateBuilder phase(GamePhase v)              { this.phase = v;            return this; }
        StateBuilder players(List<Player> v)          { this.players = v;          return this; }
        StateBuilder currentPlayerId(PlayerId v)     { this.currentPlayerId = v;  return this; }
        StateBuilder hands(Map<PlayerId, Hand> v)    { this.hands = v;            return this; }
        StateBuilder completedTricks(List<Trick> v)  { this.completedTricks = v;  return this; }
        StateBuilder currentTrick(Trick v)           { this.currentTrick = v;     return this; }
        StateBuilder trutChallenge(TrutChallenge v)  { this.trutChallenge = v;    return this; }
        StateBuilder score(Map<Team, TokenCount> v)  { this.score = v;            return this; }
        StateBuilder winner(Team v)                  { this.winner = v;           return this; }
        StateBuilder rematchVotes(Set<PlayerId> v)   { this.rematchVotes = v;     return this; }

        GameState build() {
            return new GameState(
                    gameId, phase, players, currentDealerId, currentPlayerId,
                    hands, talon, completedTricks, currentTrick, trutChallenge,
                    score, roundNumber, fortialActive, winner, rematchVotes);
        }
    }
}
