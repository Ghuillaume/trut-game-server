package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.ActionCommand;
import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.in.ApplyActionUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.action.BrellanAction;
import com.trutgame.server.domain.action.CallAction;
import com.trutgame.server.domain.action.DeuxPareillesAction;
import com.trutgame.server.domain.action.FoldAction;
import com.trutgame.server.domain.action.GameAction;
import com.trutgame.server.domain.action.PlayCardAction;
import com.trutgame.server.domain.action.TrutAction;
import com.trutgame.server.domain.model.Card;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.TokenCount;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.AiPlayerStrategy;
import com.trutgame.server.domain.service.TrutGameEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApplyActionService implements ApplyActionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyActionService.class);
    private static final long AI_DELAY_MS = 2000;

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final TrutGameEngine engine;
    private final GameViewBuilder viewBuilder;
    private final AiPlayerStrategy aiStrategy;
    private final ScheduledExecutorService scheduler;

    public ApplyActionService(GameSessionRepository repository, GameViewPublisher publisher,
                              TrutGameEngine engine, GameViewBuilder viewBuilder,
                              AiPlayerStrategy aiStrategy, ScheduledExecutorService scheduler) {
        this.repository = repository;
        this.publisher = publisher;
        this.engine = engine;
        this.viewBuilder = viewBuilder;
        this.aiStrategy = aiStrategy;
        this.scheduler = scheduler;
    }

    @Override
    public void applyAction(ActionCommand command) {
        GameState state = repository.findById(command.gameId())
            .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        PlayerId playerId = new PlayerId(command.playerId());

        GameState newState;
        String event;

        if ("START_NEW_ROUND".equals(command.actionType())) {
            newState = engine.startNewRound(state);
            event = "Nouvelle manche !";
        } else if ("REMATCH".equals(command.actionType())) {
            newState = handleRematch(state, playerId);
            event = state.getPlayer(playerId).pseudo() + " veut une revanche !";
            if (newState.phase() == GamePhase.PLAYING_TRICK || newState.phase() == GamePhase.FORTIAL_DECISION) {
                event = "Revanche acceptée ! Nouvelle partie !";
            }
        } else {
            GameAction action = mapAction(command, playerId);
            newState = engine.apply(state, action);
            event = describeAction(action, state);
        }

        repository.save(newState);
        publishViewsToAll(newState);
        publisher.publishEvent(newState.gameId(), event);

        // Schedule AI turns asynchronously with delay
        scheduleNextAiTurn(newState.gameId());
    }

    private GameState handleRematch(GameState state, PlayerId playerId) {
        if (state.phase() != GamePhase.GAME_OVER) {
            throw new IllegalStateException("Rematch only available when game is over");
        }

        Set<PlayerId> votes = new HashSet<>(state.rematchVotes());
        votes.add(playerId);

        // AI players auto-vote
        for (Player p : state.players()) {
            if (p.isAi()) {
                votes.add(p.id());
            }
        }

        // Check if all human players have voted
        long humanCount = state.players().stream().filter(p -> !p.isAi()).count();
        long humanVotes = votes.stream()
            .filter(vid -> state.players().stream()
                .anyMatch(p -> p.id().equals(vid) && !p.isAi()))
            .count();

        if (humanVotes >= humanCount) {
            // All humans voted, restart
            return createRematchState(state);
        }

        // Not all voted yet, just record vote
        return new GameState(
            state.gameId(), state.phase(), state.players(), state.currentDealerId(),
            state.currentPlayerId(), state.hands(), state.talon(), state.completedTricks(),
            state.currentTrick(), state.trutChallenge(), state.score(), state.roundNumber(),
            state.fortialActive(), state.winner(), votes
        );
    }

    private GameState createRematchState(GameState state) {
        Map<PlayerId, Hand> emptyHands = new HashMap<>();
        state.players().forEach(p -> emptyHands.put(p.id(), Hand.empty()));

        GameState freshState = new GameState(
            state.gameId(), GamePhase.WAITING_FOR_PLAYERS, state.players(),
            null, null, emptyHands, List.of(), List.of(), Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            0, false, null, Set.of()
        );

        return engine.startNewRound(freshState);
    }

    /**
     * Schedules the next AI turn with a delay to simulate thinking time.
     * Re-reads state from repository to handle concurrent modifications.
     */
    private void scheduleNextAiTurn(String gameId) {
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

                // Chain: schedule next AI turn if needed
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

    private boolean hasAnyAi(GameState state) {
        return state.players().stream().anyMatch(Player::isAi);
    }

    private void publishViewsToAll(GameState state) {
        for (Player player : state.players()) {
            GameView view = viewBuilder.buildView(state, player.id());
            publisher.publishGameView(state.gameId(), player.id(), view);
        }
    }

    private GameAction mapAction(ActionCommand command, PlayerId playerId) {
        return switch (command.actionType()) {
            case "PLAY_CARD" -> new PlayCardAction(playerId, Card.fromId(command.cardId()));
            case "TRUT" -> new TrutAction(playerId);
            case "CALL" -> new CallAction(playerId);
            case "FOLD" -> new FoldAction(playerId);
            case "BRELLAN" -> new BrellanAction(playerId);
            case "DEUX_PAREILLES" -> new DeuxPareillesAction(playerId);
            default -> throw new IllegalArgumentException("Unknown action: " + command.actionType());
        };
    }

    private String describeAction(GameAction action, GameState state) {
        String pseudo;
        try {
            pseudo = state.getPlayer(action.playerId()).pseudo();
        } catch (IllegalArgumentException e) {
            pseudo = action.playerId().value();
        }
        return switch (action) {
            case PlayCardAction a -> pseudo + " a joué " + a.card().id();
            case TrutAction ignored -> pseudo + " a truté !";
            case CallAction ignored -> pseudo + " va voir";
            case FoldAction ignored -> pseudo + " se couche";
            case BrellanAction ignored -> pseudo + " annonce Brelan !";
            case DeuxPareillesAction ignored -> pseudo + " annonce Deux pareilles une fausse !";
        };
    }
}
