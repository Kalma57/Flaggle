package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.MatchBL.PvpGameMode;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoomStatus;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * A single real 1v1 "vs Friend" capital-quiz room, kept alive in memory by
 * {@link CapitalQuizRoomRegistryBL} for as long as it's active - mirrors
 * {@link com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobePvpMatchRoom} exactly,
 * just wired to the quiz PvP engines instead of the globe ones. Also carries the
 * {@link CapitalQuizMode} (flag-to-capital vs capital-to-country), fixed at creation, since one
 * room is always one specific quiz direction.
 */
public class CapitalPvpMatchRoom implements java.io.Serializable {

    private final String roomCode;
    private final CapitalQuizMode mode;
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
    private volatile PvpCapitalQuizEngineBL engine;
    /** Created once player 2 joins, only when {@link #gameMode} is BLITZ. */
    private volatile PvpCapitalBlitzEngineBL blitzEngine;

    private CapitalPvpMatchRoom(String roomCode, CapitalQuizMode mode, PvpGameMode gameMode,
                                 int bestOf, int durationSeconds, String player1Token, CountryController cc) {
        this.roomCode = roomCode;
        this.mode = mode;
        this.gameMode = gameMode;
        this.bestOf = bestOf;
        this.pointsToWin = bestOf > 0 ? (bestOf + 1) / 2 : 0;
        this.durationSeconds = durationSeconds;
        this.player1Token = player1Token;
        this.cc = cc;
        this.status = PvpRoomStatus.WAITING_FOR_OPPONENT;
        touch();
    }

    public static CapitalPvpMatchRoom createBestOf(String roomCode, CapitalQuizMode mode, int bestOf, String player1Token, CountryController cc) {
        return new CapitalPvpMatchRoom(roomCode, mode, PvpGameMode.BEST_OF_N, bestOf, 0, player1Token, cc);
    }

    public static CapitalPvpMatchRoom createBlitz(String roomCode, CapitalQuizMode mode, int durationSeconds, String player1Token, CountryController cc) {
        return new CapitalPvpMatchRoom(roomCode, mode, PvpGameMode.BLITZ, 0, durationSeconds, player1Token, cc);
    }

    /**
     * Attaches the second real player to this room and immediately starts the match, creating
     * whichever engine matches this room's {@link #gameMode}.
     *
     * @return true if this call actually attached player 2 and started the match;
     *         false if the room was already full or the match already started/finished
     */
    public synchronized boolean joinPlayer2(String token) {
        if (status != PvpRoomStatus.WAITING_FOR_OPPONENT || player2Token != null) return false;
        this.player2Token = token;

        if (gameMode == PvpGameMode.BLITZ) {
            this.blitzEngine = new PvpCapitalBlitzEngineBL(cc, mode, durationSeconds);
            this.blitzEngine.startMatch();
        } else {
            this.engine = new PvpCapitalQuizEngineBL(cc, mode, pointsToWin);
            this.engine.startMatch();
        }

        this.status = PvpRoomStatus.IN_PROGRESS;
        touch();
        return true;
    }

    public int slotForToken(String token) {
        if (token == null) return 0;
        if (token.equals(player1Token)) return 1;
        if (token.equals(player2Token)) return 2;
        return 0;
    }

    public void touch() {
        lastActivityMillis = System.currentTimeMillis();
    }

    public boolean isStale(long staleMillis) {
        return System.currentTimeMillis() - lastActivityMillis > staleMillis;
    }

    public PvpRoomStatus getStatus() {
        if (gameMode == PvpGameMode.BLITZ) {
            if (blitzEngine != null && blitzEngine.isMatchOver()) return PvpRoomStatus.FINISHED;
        } else {
            if (engine != null && engine.isMatchOver()) return PvpRoomStatus.FINISHED;
        }
        return status;
    }

    public String getRoomCode() { return roomCode; }
    public CapitalQuizMode getMode() { return mode; }
    public PvpGameMode getGameMode() { return gameMode; }
    public int getBestOf() { return bestOf; }
    public int getPointsToWin() { return pointsToWin; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getPlayer1Token() { return player1Token; }
    public String getPlayer2Token() { return player2Token; }
    public PvpCapitalQuizEngineBL getEngine() { return engine; }
    public PvpCapitalBlitzEngineBL getBlitzEngine() { return blitzEngine; }
}
