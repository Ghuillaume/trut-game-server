package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.action.BrellanAction;
import com.trutgame.server.domain.action.CallAction;
import com.trutgame.server.domain.action.DeuxPareillesAction;
import com.trutgame.server.domain.action.FoldAction;
import com.trutgame.server.domain.action.GameAction;
import com.trutgame.server.domain.action.PlayCardAction;
import com.trutgame.server.domain.action.TrutAction;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.AiPlayerStrategy;
import com.trutgame.server.domain.service.TrutGameEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules and executes AI turns with a configurable delay.
 * Used by both StartGameService (initial AI turn) and ApplyActionService (chained AI turns).
 */
public class AiTurnScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiTurnScheduler.class);
    static final long AI_DELAY_MS = 4000;

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final TrutGameEngine engine;
    private final GameViewBuilder viewBuilder;
    private final AiPlayerStrategy aiStrategy;
    private final ScheduledExecutorService scheduler;

    public AiTurnScheduler(GameSessionRepository repository, GameViewPublisher publisher,
                           TrutGameEngine engine, GameViewBuilder viewBuilder,
                           AiPlayerStrategy aiStrategy, ScheduledExecutorService scheduler) {
        this.repository = repository;
        this.publisher = publisher;
        this.engine = engine;
        this.viewBuilder = viewBuilder;
        this.aiStrategy = aiStrategy;
        this.scheduler = scheduler;
    }

    /**
     * Schedules the next AI turn if the current player is an AI.
     * No-ops if it is a human's turn or the game is over.
     */
    public void scheduleNextAiTurn(String gameId) {
        log.info("🤖 Scheduling AI turn for game {} in {}ms", gameId, AI_DELAY_MS);
        scheduler.schedule(() -> {
            try {
                GameState state = repository.findById(gameId).orElse(null);
                if (state == null || !isAiTurn(state)) {
                    log.info("🤖 AI turn skipped for game {} (not AI turn or game not found)", gameId);
                    return;
                }

                PlayerId aiPlayerId = state.currentPlayerId();
                log.info("🤖 AI {} playing in game {}", aiPlayerId.value(), gameId);
                GameAction aiAction = aiStrategy.chooseAction(state, aiPlayerId);
                GameState newState = engine.apply(state, aiAction);
                repository.save(newState);

                String aiEvent = describeAction(aiAction, state);
                publishViewsToAll(newState);
                publisher.publishEvent(newState.gameId(), aiEvent);

                // Chain: schedule next turn if the AI plays again
                scheduleNextAiTurn(gameId);
            } catch (Exception e) {
                log.error("🤖 Error during AI turn for game {}", gameId, e);
            }
        }, AI_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private boolean isAiTurn(GameState state) {
        if (state.phase() == GamePhase.GAME_OVER
                || state.phase() == GamePhase.WAITING_FOR_PLAYERS
                || state.phase() == GamePhase.DEALING
                || state.phase() == GamePhase.END_OF_ROUND) {
            return false;
        }
        if (state.currentPlayerId() == null) return false;
        return state.players().stream()
                .anyMatch(p -> p.id().equals(state.currentPlayerId()) && p.isAi());
    }

    private void publishViewsToAll(GameState state) {
        for (Player player : state.players()) {
            GameView view = viewBuilder.buildView(state, player.id());
            publisher.publishGameView(state.gameId(), player.id(), view);
        }
    }

    private String describeAction(GameAction action, GameState state) {
        String pseudo;
        try {
            pseudo = state.getPlayer(action.playerId()).pseudo();
        } catch (IllegalArgumentException e) {
            pseudo = action.playerId().value();
        }
        return switch (action) {
            case PlayCardAction a -> pseudo + " a joué le " + cardLabel(a.card().id());
            case TrutAction ignored -> pseudo + " a truté !";
            case CallAction ignored -> pseudo + " va voir";
            case FoldAction ignored -> pseudo + " se couche";
            case BrellanAction ignored -> pseudo + " annonce Brelan !";
            case DeuxPareillesAction ignored -> pseudo + " annonce Deux pareilles une fausse !";
        };
    }

    private String cardLabel(String cardId) {
        Map<String, String> values = Map.of(
                "SEVEN", "7", "EIGHT", "8", "ACE", "As", "KING", "Roi",
                "QUEEN", "Dame", "JACK", "Valet", "TEN", "10", "NINE", "9");
        Map<String, String> suits = Map.of(
                "HEARTS", "♥", "DIAMONDS", "♦", "CLUBS", "♣", "SPADES", "♠");
        String[] parts = cardId.split("_");
        return values.getOrDefault(parts[0], parts[0]) + " de " + suits.getOrDefault(parts[1], parts[1]);
    }
}
