package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.DataAccessLayer.CountryController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-managed singleton registry of every live {@link CapitalGlobePvpMatchRoom}, keyed by a
 * short human-typeable room code - mirrors
 * {@link com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeRoomRegistryBL} exactly, just
 * kept as a separate registry/map so Capital Globe rooms never collide with plain Globe or
 * Flaggle rooms and none of those working registries are ever touched.
 */
@Service
public class CapitalGlobeRoomRegistryBL {

    private static final long STALE_ROOM_MILLIS = 2 * 60 * 1000L; // 2 minutes

    // Uppercase letters + digits, minus visually-ambiguous characters (0/O, 1/I/L).
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final CountryController cc;
    private final Map<String, CapitalGlobePvpMatchRoom> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public CapitalGlobeRoomRegistryBL(CountryController cc) {
        this.cc = cc;
    }

    /**
     * Creates a brand new "Best of N" room in WAITING_FOR_OPPONENT status, generating a
     * fresh room code (regenerating on the rare collision).
     */
    public CapitalGlobePvpMatchRoom createRoom(int bestOf, String player1Token) {
        synchronized (rooms) {
            String code;
            do {
                code = generateCode();
            } while (rooms.containsKey(code));

            CapitalGlobePvpMatchRoom room = CapitalGlobePvpMatchRoom.createBestOf(code, bestOf, player1Token, cc);
            rooms.put(code, room);
            return room;
        }
    }

    /**
     * Creates a brand new Blitz room in WAITING_FOR_OPPONENT status, generating a fresh
     * room code (regenerating on the rare collision).
     */
    public CapitalGlobePvpMatchRoom createBlitzRoom(int durationSeconds, String player1Token) {
        synchronized (rooms) {
            String code;
            do {
                code = generateCode();
            } while (rooms.containsKey(code));

            CapitalGlobePvpMatchRoom room = CapitalGlobePvpMatchRoom.createBlitz(code, durationSeconds, player1Token, cc);
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
    public CapitalGlobePvpMatchRoom getRoom(String roomCode) {
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
