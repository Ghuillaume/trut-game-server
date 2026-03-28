package com.trutgame.server.domain.service;

import com.trutgame.server.domain.action.*;
import com.trutgame.server.domain.model.*;
import com.trutgame.server.domain.phase.GamePhase;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Stateless AI strategy for choosing actions in a Trut game.
 */
public class AiPlayerStrategy {

    private static final Set<CardValue> TOP_3 = Set.of(CardValue.SEVEN, CardValue.EIGHT, CardValue.ACE);
    private static final Set<CardValue> TOP_4 = Set.of(CardValue.SEVEN, CardValue.EIGHT, CardValue.ACE, CardValue.KING);

    public GameAction chooseAction(GameState state, PlayerId aiPlayerId) {
        return switch (state.phase()) {
            case PLAYING_TRICK -> choosePlayingAction(state, aiPlayerId);
            case TRUT_CHALLENGE -> chooseTrutResponse(state, aiPlayerId);
            case FORTIAL_DECISION -> chooseFortialDecision(state, aiPlayerId);
            default -> throw new IllegalStateException("AI cannot act in phase: " + state.phase());
        };
    }

    private GameAction choosePlayingAction(GameState state, PlayerId aiPlayerId) {
        Hand hand = state.hands().get(aiPlayerId);
        if (hand == null || hand.size() == 0) {
            throw new IllegalStateException("AI has no cards to play");
        }

        // If it's the AI's turn to play a card
        if (aiPlayerId.equals(state.currentPlayerId())) {
            Card cardToPlay = chooseCard(state, aiPlayerId, hand);
            return new PlayCardAction(aiPlayerId, cardToPlay);
        }

        // Not AI's turn to play, but can trut if no challenge active
        if (state.trutChallenge() == null) {
            long topCards = hand.cards().stream()
                .filter(c -> TOP_3.contains(c.value()))
                .count();
            if (topCards >= 2) {
                return new TrutAction(aiPlayerId);
            }
        }

        // Fallback: shouldn't normally reach here
        throw new IllegalStateException("AI cannot determine action");
    }

    private Card chooseCard(GameState state, PlayerId aiPlayerId, Hand hand) {
        Team aiTeam = state.getTeam(aiPlayerId);
        Trick currentTrick = state.currentTrick();

        if (currentTrick.size() > 0) {
            // Check if partner is currently winning the trick
            boolean partnerWinning = isPartnerWinning(currentTrick, state.players(), aiTeam);
            if (partnerWinning) {
                // Play lowest card to save strong cards
                return hand.cards().stream()
                    .max(Comparator.comparingInt(c -> c.value().rank()))
                    .orElse(hand.cards().get(0));
            }
        }

        // Play highest card (lowest rank = strongest)
        return hand.cards().stream()
            .min(Comparator.comparingInt(c -> c.value().rank()))
            .orElse(hand.cards().get(0));
    }

    private boolean isPartnerWinning(Trick trick, List<Player> players, Team aiTeam) {
        if (trick.entries().isEmpty()) return false;

        int bestRank = Integer.MAX_VALUE;
        Team bestTeam = null;
        for (TrickEntry entry : trick.entries()) {
            Player player = players.stream()
                .filter(p -> p.id().equals(entry.playerId()))
                .findFirst().orElse(null);
            if (player != null && entry.card().value().rank() < bestRank) {
                bestRank = entry.card().value().rank();
                bestTeam = player.team();
            }
        }
        return bestTeam == aiTeam;
    }

    private GameAction chooseTrutResponse(GameState state, PlayerId aiPlayerId) {
        Hand hand = state.hands().get(aiPlayerId);
        if (hand != null) {
            boolean hasStrongCard = hand.cards().stream()
                .anyMatch(c -> TOP_4.contains(c.value()));
            if (hasStrongCard) {
                return new CallAction(aiPlayerId);
            }
        }
        // Fold if no strong card (but not during fortial)
        if (state.fortialActive()) {
            return new CallAction(aiPlayerId);
        }
        return new FoldAction(aiPlayerId);
    }

    private GameAction chooseFortialDecision(GameState state, PlayerId aiPlayerId) {
        Hand hand = state.hands().get(aiPlayerId);
        if (hand != null) {
            long topCards = hand.cards().stream()
                .filter(c -> TOP_3.contains(c.value()))
                .count();
            if (topCards >= 2) {
                return new TrutAction(aiPlayerId);
            }
        }
        return new FoldAction(aiPlayerId);
    }
}
