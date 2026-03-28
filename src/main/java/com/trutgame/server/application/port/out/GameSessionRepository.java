package com.trutgame.server.application.port.out;

import com.trutgame.server.domain.model.GameState;

import java.util.Optional;
import java.util.function.UnaryOperator;

public interface GameSessionRepository {
    void save(GameState state);
    Optional<GameState> findById(String gameId);
    GameState update(String gameId, UnaryOperator<GameState> transition);
    void delete(String gameId);
}
