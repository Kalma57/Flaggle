package com.example.flagdemo.BusinessLayer.GlobeBL;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Shared letter-masking logic behind every Globe hint system - the single-player
 * {@link GlobeEngineBL#useHint()}, and the Best-of match engines' own hints. Each hint reveals
 * one additional letter at a RANDOM not-yet-revealed position (not left-to-right), so an early
 * hint can't give away short/obvious names by exposing an easy prefix. Non-letter characters
 * (spaces, hyphens, apostrophes) are always shown.
 */
public final class CountryNameHintUtil {

    private CountryNameHintUtil() {}

    private static final Random RANDOM = new Random();

    public static int countRevealableLetters(String name) {
        int count = 0;
        for (char c : name.toCharArray()) {
            if (Character.isLetter(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Reveals one additional random not-yet-revealed letter position, mutating
     * {@code revealedPositions} in place. No-op once every letter is already revealed.
     */
    public static void revealRandomLetter(String name, Set<Integer> revealedPositions) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i)) && !revealedPositions.contains(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return;
        revealedPositions.add(candidates.get(RANDOM.nextInt(candidates.size())));
    }

    public static String maskName(String name, Set<Integer> revealedPositions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(revealedPositions.contains(i) ? c : '_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
