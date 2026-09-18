package com.example.flagdemo.BusinessLayer.DailyQuizBL;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;

/**
 * Marks whether this browser already played today's Daily Quiz, via a cookie
 * stamped with today's date. This is a convenience gate, not real anti-cheat
 * (clearing cookies or a private window resets it) - the same trade-off every
 * daily-puzzle game like this makes. Fully bypassed while
 * {@link DailyQuizDevConfig#DEV_MODE_ENABLED} is true.
 */
public final class DailyQuizPlayedGuardBL {

    private static final String COOKIE_NAME = "dailyQuizPlayed";

    private DailyQuizPlayedGuardBL() {}

    public static boolean hasPlayedToday(HttpServletRequest request) {
        if (DailyQuizDevConfig.DEV_MODE_ENABLED) return false;

        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        String today = LocalDate.now().toString();
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && today.equals(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static void markPlayedToday(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, LocalDate.now().toString());
        cookie.setMaxAge(60 * 60 * 30); // ~30h - comfortably past midnight even across timezones
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /** Dev-only: lets the debug endpoint reset the gate without clearing cookies by hand. */
    public static void clearPlayed(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
