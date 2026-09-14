package com.example.flagdemo.BusinessLayer.CapitalBL;

/**
 * A precomputed "decision" for the AI on a single quiz round - no live thread or agent loop,
 * same philosophy as {@link com.example.flagdemo.BusinessLayer.MatchBL.AiOpponentPlan} and
 * {@link com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeAiOpponentPlan}: everything the
 * AI will "do" this round is decided once, up front, and every later check just compares
 * elapsed time against it.
 *
 * Unlike those two (which simulate a multi-attempt narrowing-in process), a multiple-choice
 * question is answered in one shot - so the AI's whole behavior collapses to just two facts:
 * when it answers, and whether that answer is correct.
 */
public class CapitalAiOpponentPlan implements java.io.Serializable {

    private final double answerTimeSeconds;
    private final boolean correct;

    public CapitalAiOpponentPlan(double answerTimeSeconds, boolean correct) {
        this.answerTimeSeconds = answerTimeSeconds;
        this.correct = correct;
    }

    public double getAnswerTimeSeconds() { return answerTimeSeconds; }

    /** Has the AI's answer "landed" yet, given how long the round has been running? */
    public boolean hasAnswered(double elapsedSeconds) {
        return elapsedSeconds >= answerTimeSeconds;
    }

    /** Only meaningful once {@link #hasAnswered(double)} is true. */
    public boolean isCorrect() { return correct; }
}
