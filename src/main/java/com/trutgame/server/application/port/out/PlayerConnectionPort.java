package com.trutgame.server.application.port.out;

import java.util.Set;

public interface PlayerConnectionPort {
    Set<String> getDisconnectedPlayers(String gameId);
    boolean isConnected(String gameId, String playerId);
}
