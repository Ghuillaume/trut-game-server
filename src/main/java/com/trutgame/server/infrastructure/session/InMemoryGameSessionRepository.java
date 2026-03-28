package com.trutgame.server.infrastructure.session;

import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.domain.model.GameState;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@Repository
public class InMemoryGameSessionRepository implements GameSessionRepository {

    private final ConcurrentHashMap<String, GameState> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(GameState state) {
        sessions.put(state.gameId(), state);
    }

    @Override
    public Optional<GameState> findById(String gameId) {
        return Optional.ofNullable(sessions.get(gameId));
    }

    @Override
    public synchronized GameState update(String gameId, UnaryOperator<GameState> transition) {
        GameState current = sessions.get(gameId);
        if (current == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        GameState updated = transition.apply(current);
        sessions.put(gameId, updated);
        return updated;
    }

    @Override
    public void delete(String gameId) {
        sessions.remove(gameId);
    }

    public int activeGameCount() {
        return sessions.size();
    }
}
