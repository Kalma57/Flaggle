package BusinessLayer;

public class GameEngineBL {
        private CountryBL targetCountry;
        private int attempts;
        private boolean gameOver;

        public GameEngineBL() {
            this.attempts = 0;
            this.gameOver = false;
        }

    /**
     * Starts a new game by selecting a random target country,
     * resetting the number of attempts and the gameOver flag.
     */
    public void StartNewGame() {
        this.targetCountry = selectRandomCountry();
        this.attempts = 0;
        this.gameOver = false;
    }

    /**
     * Processes a guess of a country name and returns the result.
     * Increments the attempt count if the game is not over.
     * Sets gameOver to true if the guess is correct.
     *
     * @param countryName the name of the country being guessed
     * @return a GuessResultBL object representing the guess result,
     *         or null if the game is already over
     */
    public GuessResultBL Guess(String countryName) {
        if (gameOver) {
            return null;
        }
        this.attempts++;

        CountryBL guessedCountry = findCountryByName(countryName);

        GuessResultBL result = new GuessResultBL(guessedCountry, targetCountry);

        if (result.isCorrect()) {
            this.gameOver = true;
        }

        return result;
    }

    /**
     * Checks whether the game is over.
     *
     * @return true if the game has ended, false otherwise
     */
    public boolean IsGameOver() {
        return gameOver;
    }

    /**
     * Returns the number of attempts made so far.
     *
     * @return the count of guesses made
     */
    public int GetAttempts() {
        return attempts;
    }

    /**
     * Selects a random country from the available countries list.
     *
     * @return a randomly selected CountryBL object
     */
    private CountryBL selectRandomCountry() {
        // Implementation needed: select a random country from the available list
    }

    /**
     * Finds a country by its name in the available countries list.
     *
     * @param name the name of the country to find
     * @return the matching CountryBL object, or null if not found
     */
    private CountryBL findCountryByName(String name) {
        // Implementation needed: find country by name from the available list
    }
}