package com.trutgame.server.infrastructure.session;

import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.domain.model.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@Repository
public class InMemoryGameSessionRepository implements GameSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryGameSessionRepository.class);
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(GameState state) {
        sessions.compute(state.gameId(), (id, existing) -> {
            if (existing != null) {
                return existing.withState(state);
            }
            return new GameSession(state, Instant.now());
        });
    }

    @Override
    public Optional<GameState> findById(String gameId) {
        GameSession session = sessions.get(gameId);
        return session != null ? Optional.of(session.state()) : Optional.empty();
    }

    @Override
    public synchronized GameState update(String gameId, UnaryOperator<GameState> transition) {
        GameSession session = sessions.get(gameId);
        if (session == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        GameState updated = transition.apply(session.state());
        sessions.put(gameId, session.withState(updated));
        return updated;
    }

    @Override
    public void delete(String gameId) {
        sessions.remove(gameId);
    }

    public int activeGameCount() {
        return sessions.size();
    }

    @Scheduled(fixedDelay = 300000)
    public void cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minus(SESSION_TTL);
        long count = sessions.entrySet().stream()
            .filter(entry -> entry.getValue().lastActivityTime().isBefore(cutoff))
            .map(entry -> sessions.remove(entry.getKey()))
            .count();
        if (count > 0) {
            log.info("🧹 Cleaned up {} expired game sessions", count);
        }
    }

    record GameSession(GameState state, Instant lastActivityTime) {
        GameSession withState(GameState newState) {
            return new GameSession(newState, Instant.now());
        }
    }
}
