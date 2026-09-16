package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.DataAccessLayer.CountryController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-managed singleton registry of every live {@link CapitalPvpMatchRoom}, keyed by a short
 * human-typeable room code - mirrors
 * {@link com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeRoomRegistryBL} exactly,
 * just kept as a separate registry/map so capital-quiz rooms never collide with the globe game's
 * (or Flaggle's) rooms.
 */
@Service
public class CapitalQuizRoomRegistryBL {

    private static final long STALE_ROOM_MILLIS = 2 * 60 * 1000L; // 2 minutes

    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final CountryController cc;
    private final Map<String, CapitalPvpMatchRoom> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public CapitalQuizRoomRegistryBL(CountryController cc) {
        this.cc = cc;
    }

    public CapitalPvpMatchRoom createRoom(CapitalQuizMode mode, int bestOf, String player1Token) {
        synchronized (rooms) {
            String code;
            do {
                code = generateCode();
            } while (rooms.containsKey(code));

            CapitalPvpMatchRoom room = CapitalPvpMatchRoom.createBestOf(code, mode, bestOf, player1Token, cc);
            rooms.put(code, room);
            return room;
        }
    }

    public CapitalPvpMatchRoom createBlitzRoom(CapitalQuizMode mode, int durationSeconds, String player1Token) {
        synchronized (rooms) {
            String code;
            do {
                code = generateCode();
            } while (rooms.containsKey(code));

            CapitalPvpMatchRoom room = CapitalPvpMatchRoom.createBlitz(code, mode, durationSeconds, player1Token, cc);
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

    public CapitalPvpMatchRoom getRoom(String roomCode) {
        if (roomCode == null) return null;
        return rooms.get(roomCode.trim().toUpperCase());
    }

    public void removeRoom(String roomCode) {
        if (roomCode == null) return;
        rooms.remove(roomCode.trim().toUpperCase());
    }

    @Scheduled(fixedRate = 30_000)
    public void sweepStaleRooms() {
        rooms.values().removeIf(room -> room.isStale(STALE_ROOM_MILLIS));
    }
}
