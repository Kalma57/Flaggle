package com.example.flagdemo.BusinessLayer.MatchBL;

import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-managed singleton registry of every live {@link PvpMatchRoom}, keyed by a
 * short human-typeable room code.
 *
 * There is no database or Redis backing this - a plain in-memory
 * {@link ConcurrentHashMap} is correct for this app's single-instance scale, exactly
 * like {@link CountryController} being the one shared singleton for country data.
 * Stale rooms (no player activity for a couple of minutes) are periodically swept out
 * by {@link #sweepStaleRooms()}.
 */
@Service
public class GameRoomRegistryBL {

    private static final long STALE_ROOM_MILLIS = 2 * 60 * 1000L; // 2 minutes

    // Uppercase letters + digits, minus visually-ambiguous characters (0/O, 1/I/L).
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final CountryController cc;
    private final Map<String, PvpMatchRoom> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public GameRoomRegistryBL(CountryController cc) {
        this.cc = cc;
    }

    /**
     * Creates a brand new "Best of N" room in WAITING_FOR_OPPONENT status, generating a
     * fresh room code (regenerating on the rare collision).
     */
    public PvpMatchRoom createRoom(DifficultyLevel difficulty, int bestOf, String player1Token) {
        synchronized (rooms) {
            String code;
            do {
                code = generateCode();
            } while (rooms.containsKey(code));

            PvpMatchRoom room = PvpMatchRoom.createBestOf(code, difficulty, bestOf, player1Token, cc);
            rooms.put(code, room);
            return room;
        }
    }

    /**
     * Creates a brand new Blitz room in WAITING_FOR_OPPONENT status, generating a fresh
     * room code (regenerating on the rare collision).
     */
    public PvpMatchRoom createBlitzRoom(DifficultyLevel difficulty, int durationSeconds, String player1Token) {
        synchronized (rooms) {
            String code;
            do {
                code = generateCode();
            } while (rooms.containsKey(code));

            PvpMatchRoom room = PvpMatchRoom.createBlitz(code, difficulty, durationSeconds, player1Token, cc);
            rooms.put(code, room);
            return room;
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /** Looks up a room by code (case-insensitive, whitespace-tolerant), or null if none exists. */
    public PvpMatchRoom getRoom(String roomCode) {
        if (roomCode == null) return null;
        return rooms.get(roomCode.trim().toUpperCase());
    }

    public void removeRoom(String roomCode) {
        if (roomCode == null) return;
        rooms.remove(roomCode.trim().toUpperCase());
    }

    /**
     * Periodic cleanup: evicts any room neither player has polled or acted in for
     * a while, so an abandoned "waiting for opponent" room or a match nobody came
     * back to doesn't linger forever.
     */
    @Scheduled(fixedRate = 30_000)
    public void sweepStaleRooms() {
        rooms.values().removeIf(room -> room.isStale(STALE_ROOM_MILLIS));
    }
}
