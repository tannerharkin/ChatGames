package dev.rarehyperion.chatgames.game.types;

import dev.rarehyperion.chatgames.ChatGamesCore;
import dev.rarehyperion.chatgames.game.AbstractGame;
import dev.rarehyperion.chatgames.game.GameConfig;
import dev.rarehyperion.chatgames.util.MessageUtil;
import dev.rarehyperion.chatgames.util.StringUtil;
import net.kyori.adventure.text.Component;

import java.util.Optional;

public class TriviaGame extends AbstractGame {

    private final GameConfig.QuestionAnswer question;

    public TriviaGame(final ChatGamesCore plugin, final GameConfig config) {
        super(plugin, config);

        final GameConfig.OpenTriviaSettings openTriviaSettings = config.getOpenTriviaSettings();

        // Try API first if enabled
        final Optional<GameConfig.QuestionAnswer> apiQuestion =
                plugin.openTriviaService().getTriviaQuestion(openTriviaSettings);

        if (apiQuestion.isPresent()) {
            this.question = apiQuestion.get();
        } else {
            this.question = this.selectRandom(config.getQuestions());
        }
    }

    @Override
    public void start() {
        this.plugin.broadcast(this.createStartMessage());
    }

    @Override
    public boolean checkAnswer(final String answer) {
        final String correctAnswer = this.question.answer();

        // Check exact match first (case-insensitive)
        if (answer.equalsIgnoreCase(correctAnswer)) {
            return true;
        }

        // Apply fuzzy matching if enabled, answer meets minimum length, and is not purely numeric
        final GameConfig.FuzzyMatchSettings fuzzySettings = this.config.getFuzzyMatchSettings();
        if (fuzzySettings.isEnabled()
                && correctAnswer.length() >= fuzzySettings.getMinLength()
                && !StringUtil.isNumeric(correctAnswer, '-', '.')) {
            final String mode = fuzzySettings.getMode();
            if ("per-word".equalsIgnoreCase(mode)) {
                return StringUtil.fuzzyMatchByWords(
                        answer,
                        correctAnswer,
                        fuzzySettings.getBaseDistance(),
                        fuzzySettings.getPerWordDistance()
                );
            } else {
                // Fixed mode - use base distance only
                return StringUtil.fuzzyMatch(
                        answer,
                        correctAnswer,
                        fuzzySettings.getBaseDistance()
                );
            }
        }

        return false;
    }

    @Override
    public Component getQuestion() {
        return MessageUtil.parse(this.question.question());
    }

    @Override
    public Optional<String> getCorrectAnswer() {
        return Optional.of(this.question.answer());
    }

}
