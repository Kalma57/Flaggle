package com.example.flagdemo.BusinessLayer.MatchBL;

import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.sql.SQLException;

/**
 * A single real 1v1 "vs Friend" game room, kept alive in memory by
 * {@link GameRoomRegistryBL} for as long as it's active.
 *
 * Holds both players' identity (opaque per-browser tokens, minted on create/join and
 * stashed in that browser's {@code HttpSession} so a page refresh doesn't lose it),
 * the room's lifecycle status, and - once both players are present - the shared engine
 * both of their browsers act on. A room hosts EITHER a "Best of N" match (via
 * {@link PvpMatchEngineBL}) OR a Blitz match (via {@link PvpBlitzEngineBL}), decided by
 * {@link #gameMode} at creation time; only the corresponding engine field is ever
 * populated.
 *
 * There is no database or distributed cache backing this - for this app's
 * single-instance scale, a plain in-memory object shared via a
 * {@code ConcurrentHashMap<String, PvpMatchRoom>} is correct and simple.
 */
public class PvpMatchRoom implements java.io.Serializable {

    private final String roomCode;
    private final DifficultyLevel difficulty;
    private final PvpGameMode gameMode;

    /** "Best of N" specific - 0 when {@link #gameMode} is {@link PvpGameMode#BLITZ}. */
    private final int bestOf;
    private final int pointsToWin;

    /** Blitz specific, in seconds - 0 when {@link #gameMode} is {@link PvpGameMode#BEST_OF_N}. */
    private final int durationSeconds;

    private final CountryController cc;

    private final String player1Token;
    private volatile String player2Token;

    private volatile PvpRoomStatus status;
    private volatile long lastActivityMillis;

    /** Created once player 2 joins, only when {@link #gameMode} is BEST_OF_N. */
    private volatile PvpMatchEngineBL engine;
    /** Created once player 2 joins, only when {@link #gameMode} is BLITZ. */
    private volatile PvpBlitzEngineBL blitzEngine;

    private PvpMatchRoom(String roomCode, DifficultyLevel difficulty, PvpGameMode gameMode,
                          int bestOf, int durationSeconds, String player1Token, CountryController cc) {
        this.roomCode = roomCode;
        this.difficulty = difficulty;
        this.gameMode = gameMode;
        this.bestOf = bestOf;
        this.pointsToWin = bestOf > 0 ? (bestOf + 1) / 2 : 0;
        this.durationSeconds = durationSeconds;
        this.player1Token = player1Token;
        this.cc = cc;
        this.status = PvpRoomStatus.WAITING_FOR_OPPONENT;
        touch();
    }

    /** Creates a new "Best of N" room (bestOf is 3, 5, or 7). */
    public static PvpMatchRoom createBestOf(String roomCode, DifficultyLevel difficulty, int bestOf,
                                             String player1Token, CountryController cc) {
        return new PvpMatchRoom(roomCode, difficulty, PvpGameMode.BEST_OF_N, bestOf, 0, player1Token, cc);
    }

    /** Creates a new Blitz room (durationSeconds is 60 or 120). */
    public static PvpMatchRoom createBlitz(String roomCode, DifficultyLevel difficulty, int durationSeconds,
                                            String player1Token, CountryController cc) {
        return new PvpMatchRoom(roomCode, difficulty, PvpGameMode.BLITZ, 0, durationSeconds, player1Token, cc);
    }

    /**
     * Attaches the second real player to this room and immediately starts the match,
     * creating whichever engine matches this room's {@link #gameMode}.
     *
     * @return true if this call actually attached player 2 and started the match;
     *         false if the room was already full or the match already started/finished
     */
    public synchronized boolean joinPlayer2(String token) throws SQLException {
        if (status != PvpRoomStatus.WAITING_FOR_OPPONENT || player2Token != null) return false;
        this.player2Token = token;

        if (gameMode == PvpGameMode.BLITZ) {
            this.blitzEngine = new PvpBlitzEngineBL(cc, difficulty, durationSeconds);
            this.blitzEngine.startMatch();
        } else {
            this.engine = new PvpMatchEngineBL(cc, difficulty, pointsToWin);
            this.engine.startMatch();
        }

        this.status = PvpRoomStatus.IN_PROGRESS;
        touch();
        return true;
    }

    /**
     * Resolves which player (1 or 2) a given browser's stored token belongs to, or
     * 0 if the token doesn't match either player currently in this room.
     */
    public int slotForToken(String token) {
        if (token == null) return 0;
        if (token.equals(player1Token)) return 1;
        if (token.equals(player2Token)) return 2;
        return 0;
    }

    public void touch() {
        lastActivityMillis = System.currentTimeMillis();
    }

    /** True if neither player has polled/acted in this room for at least {@code staleMillis}. */
    public boolean isStale(long staleMillis) {
        return System.currentTimeMillis() - lastActivityMillis > staleMillis;
    }

    /**
     * The room's current lifecycle status. FINISHED is derived on read (once whichever
     * engine is active reports the match over) rather than tracked as a separate
     * mutable field, so there's nothing extra to keep in sync.
     */
    public PvpRoomStatus getStatus() {
        if (gameMode == PvpGameMode.BLITZ) {
            if (blitzEngine != null && blitzEngine.isMatchOver()) return PvpRoomStatus.FINISHED;
        } else {
            if (engine != null && engine.isMatchOver()) return PvpRoomStatus.FINISHED;
        }
        return status;
    }

    public String getRoomCode() { return roomCode; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public PvpGameMode getGameMode() { return gameMode; }
    public int getBestOf() { return bestOf; }
    public int getPointsToWin() { return pointsToWin; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getPlayer1Token() { return player1Token; }
    public String getPlayer2Token() { return player2Token; }
    public PvpMatchEngineBL getEngine() { return engine; }
    public PvpBlitzEngineBL getBlitzEngine() { return blitzEngine; }
}
