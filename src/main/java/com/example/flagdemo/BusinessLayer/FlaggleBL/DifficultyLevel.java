package com.example.flagdemo.BusinessLayer.FlaggleBL;

/**
 * Represents how much visual information the player receives while guessing
 * in the Flaggle game.
 *
 * HARD - every guess is shown independently: areas that match the target are
 *        green, everything else is plain black. No information about the
 *        target flag's real colors is ever revealed, and nothing carries over
 *        between guesses.
 *
 * EASY - the target flag's picture starts out solid black. Whenever an area
 *        of a guess matches the target, that area is permanently revealed
 *        using the target flag's true (real, un-normalized) color. This
 *        builds up across every guess made in the round, so previously
 *        revealed areas never go back to black — the player gradually
 *        uncovers what the correct flag actually looks like.
 */
public enum DifficultyLevel {
    EASY,
    HARD
}
