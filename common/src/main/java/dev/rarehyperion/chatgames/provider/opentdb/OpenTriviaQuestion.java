package dev.rarehyperion.chatgames.provider.opentdb;

import dev.rarehyperion.chatgames.game.GameConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal model for questions fetched from the Open Trivia DB API.
 */
public final class OpenTriviaQuestion {

    private final String category;
    private final String difficulty;
    private final String question;
    private final String correctAnswer;
    private final List<String> incorrectAnswers;

    public OpenTriviaQuestion(
            final String category,
            final String difficulty,
            final String question,
            final String correctAnswer,
            final List<String> incorrectAnswers
    ) {
        this.category = category;
        this.difficulty = difficulty;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.incorrectAnswers = incorrectAnswers != null ? incorrectAnswers : Collections.emptyList();
    }

    public String category() {
        return this.category;
    }

    public String difficulty() {
        return this.difficulty;
    }

    public String question() {
        return this.question;
    }

    public String correctAnswer() {
        return this.correctAnswer;
    }

    public List<String> incorrectAnswers() {
        return this.incorrectAnswers;
    }

    /**
     * Check if this is a true/false question.
     *
     * @return true if the answer is "True" or "False"
     */
    public boolean isTrueFalse() {
        return "True".equalsIgnoreCase(this.correctAnswer) || "False".equalsIgnoreCase(this.correctAnswer);
    }

    /**
     * Check if this question is suitable for free-form text trivia.
     * Questions containing certain phrases typically require multiple choice
     * options or imply multiple answers, so they are filtered out conservatively.
     * Answers longer than a few words are also filtered as they're nearly
     * impossible to guess exactly.
     *
     * @param maxAnswerWords Maximum number of words allowed in the answer
     * @return true if the question works as free-form trivia
     */
    public boolean isSuitableForTrivia(final int maxAnswerWords) {
        final String lowerQuestion = this.question.toLowerCase();

        // Filter problematic question phrasings
        if (lowerQuestion.contains("which")
                || lowerQuestion.contains("what are")
                || lowerQuestion.contains("is not")) {
            return false;
        }

        // Filter answers that are too long to reasonably guess
        final int answerWords = countWords(this.correctAnswer);
        return answerWords <= maxAnswerWords;
    }

    private static int countWords(final String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
        return str.trim().split("\\s+").length;
    }

    /**
     * Convert this API question to a GameConfig.QuestionAnswer for trivia games.
     * For true/false questions, adds the provided prefix for clarity.
     *
     * @param trueFalsePrefix The prefix to add for true/false questions (e.g., "True or false?")
     * @return A QuestionAnswer with the question text and correct answer
     */
    public GameConfig.QuestionAnswer toQuestionAnswer(final String trueFalsePrefix) {
        final String formattedQuestion;
        if (isTrueFalse() && trueFalsePrefix != null && !trueFalsePrefix.isEmpty()) {
            formattedQuestion = trueFalsePrefix + " " + this.question;
        } else {
            formattedQuestion = this.question;
        }
        return new GameConfig.QuestionAnswer(formattedQuestion, this.correctAnswer);
    }

    /**
     * Convert this API question to a GameConfig.MultipleChoiceQuestion.
     * Shuffles answers and formats them with letter prefixes (A., B., etc.).
     *
     * @return A MultipleChoiceQuestion ready for use in games
     */
    public GameConfig.MultipleChoiceQuestion toMultipleChoiceQuestion() {
        final List<String> allAnswers = new ArrayList<>();
        allAnswers.add(this.correctAnswer);
        allAnswers.addAll(this.incorrectAnswers);
        Collections.shuffle(allAnswers);

        // Find which letter is correct after shuffle
        final char correctLetter = (char) ('A' + allAnswers.indexOf(this.correctAnswer));

        // Format as "A. Answer text"
        final List<String> formatted = new ArrayList<>();
        for (int i = 0; i < allAnswers.size(); i++) {
            formatted.add((char) ('A' + i) + ". " + allAnswers.get(i));
        }

        return new GameConfig.MultipleChoiceQuestion(
                this.question,
                formatted,
                String.valueOf(correctLetter)
        );
    }

}
