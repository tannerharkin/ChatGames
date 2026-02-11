package dev.rarehyperion.chatgames.game;

import dev.rarehyperion.chatgames.config.Config;
import dev.rarehyperion.chatgames.util.MessageUtil;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GameConfig {

    private final String name, displayName;

    private final GameType type;
    private final int timeoutSeconds;

    private final List<String> rewardCommands;

    private final String startMessage, winMessage, timeoutMessage;

    private final List<String> words;
    private final List<QuestionAnswer> questions;
    private final List<ReactionVariant> reactionVariants;
    private final List<MultipleChoiceQuestion> multipleChoiceQuestions;

    private final FuzzyMatchSettings fuzzyMatchSettings;

    public GameConfig(final String type, final Config configuration) {
        this.name = configuration.getString("name", "Unknown");
        this.displayName = configuration.getString("display-name", this.name);

        this.type = GameType.fromId(type);
        this.timeoutSeconds = configuration.getInt("timeout", 60);

        this.rewardCommands = configuration.getStringList("reward-commands");

        this.startMessage = configuration.getString("messages.start", "<red>Failed to fetch message, report this to a server administrator.</red>");
        this.winMessage = configuration.getString("messages.win", "<red>Failed to fetch message, report this to a server administrator.</red>");
        this.timeoutMessage = configuration.getString("messages.timeout", "<red>Failed to fetch message, report this to a server administrator.</red>");

        this.words = configuration.getStringList("words");
        this.questions = this.loadQuestions(configuration.getList("questions"));
        this.reactionVariants = this.loadReactionVariants(configuration.getList("variants"));
        this.multipleChoiceQuestions = this.loadMultipleChoiceQuestions(configuration.getConfigurationSection("questions"));

        this.fuzzyMatchSettings = this.loadFuzzyMatchSettings(configuration.getConfigurationSection("fuzzy-matching"));
    }

    private List<QuestionAnswer> loadQuestions(final List<?> list) {
        final List<QuestionAnswer> result = new ArrayList<>();
        if(list == null) return result;

        for(final Object obj : list) {
            if(obj instanceof List<?>) {
                final List<?> pair = (List<?>) obj;

                if(pair.size() >= 2) {
                    result.add(new QuestionAnswer(
                            pair.get(0).toString(),
                            pair.get(1).toString()
                    ));
                }
            }
        }

        return result;
    }

    private List<ReactionVariant> loadReactionVariants(final List<?> list) {
        final List<ReactionVariant> result = new ArrayList<>();
        if(list == null) return result;

        for(final Object obj : list) {
            if(obj instanceof Map<?,?>) {
                final Map<?, ?> map = (Map<?, ?>) obj;

                result.add(new ReactionVariant(
                        String.valueOf(map.get("name")),
                        String.valueOf(map.get("challenge")),
                        String.valueOf(map.get("answer"))
                ));
            }
        }

        return result;
    }

    private List<MultipleChoiceQuestion> loadMultipleChoiceQuestions(final Config section) {
        final List<MultipleChoiceQuestion> result = new ArrayList<>();
        if(section == null) return result;

        for(final String key : section.getKeys(false)) {
            final Config questionSection = section.getConfigurationSection(key);
            if(questionSection == null) continue;

            result.add(new MultipleChoiceQuestion(
                    questionSection.getString("question", ""),
                    questionSection.getStringList("answers"),
                    questionSection.getString("correct-answer", "")
            ));
        }

        return result;
    }

    public Component getStartMessage(final Component question) {
        return MessageUtil.parse(this.startMessage
                .replace("{name}", this.displayName)
                .replace("{timeout}", String.valueOf(this.timeoutSeconds))
        ).append(Component.newline()).append(question);
    }

    public Component getWinMessage(final String playerName, final String answer) {
        return MessageUtil.parse(this.winMessage
                .replace("{player}", playerName)
                .replace("{name}", this.displayName)
                .replace("{answer}", answer)
        );
    }

    public Component getTimeoutMessage(final String answer) {
        return MessageUtil.parse(this.timeoutMessage
                .replace("{name}", this.displayName)
                .replace("{answer}", answer)
        );
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public GameType getType() {
        return this.type;
    }

    public int getTimeoutSeconds() {
        return this.timeoutSeconds;
    }

    public List<String> getRewardCommands() {
        return this.rewardCommands;
    }

    public List<String> getWords() {
        return this.words;
    }

    public List<QuestionAnswer> getQuestions() {
        return this.questions;
    }

    public List<ReactionVariant> getReactionVariants() {
        return this.reactionVariants;
    }

    public List<MultipleChoiceQuestion> getMultipleChoiceQuestions() {
        return this.multipleChoiceQuestions;
    }

    public OpenTriviaSettings getOpenTriviaSettings() {
        return this.openTriviaSettings;
    }

    public FuzzyMatchSettings getFuzzyMatchSettings() {
        return this.fuzzyMatchSettings;
    }

    private FuzzyMatchSettings loadFuzzyMatchSettings(final Config section) {
        if (section == null) {
            return new FuzzyMatchSettings(false, 4, "per-word", 1, 1);
        }

        final boolean enabled = section.getBoolean("enabled", false);
        final int minLength = section.getInt("min-length", 4);
        final String mode = section.getString("mode", "per-word");
        final int baseDistance = section.getInt("base-distance", 1);
        final int perWordDistance = section.getInt("per-word-distance", 1);

        return new FuzzyMatchSettings(enabled, minLength, mode, baseDistance, perWordDistance);
    }

    public static final class FuzzyMatchSettings {

        private final boolean enabled;
        private final int minLength;
        private final String mode;
        private final int baseDistance;
        private final int perWordDistance;

        public FuzzyMatchSettings(
                final boolean enabled,
                final int minLength,
                final String mode,
                final int baseDistance,
                final int perWordDistance
        ) {
            this.enabled = enabled;
            this.minLength = minLength;
            this.mode = mode;
            this.baseDistance = baseDistance;
            this.perWordDistance = perWordDistance;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public int getMinLength() {
            return this.minLength;
        }

        public String getMode() {
            return this.mode;
        }

        public int getBaseDistance() {
            return this.baseDistance;
        }

        public int getPerWordDistance() {
            return this.perWordDistance;
        }

    }

    public static final class QuestionAnswer {

        private final String question;
        private final String answer;

        public QuestionAnswer(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String question() {
            return this.question;
        }

        public String answer() {
            return this.answer;
        }

    }

    public static final class ReactionVariant {

        private final String name;
        private final String challenge;
        private final String answer;

        public ReactionVariant(String name, String challenge, String answer) {
            this.name = name;
            this.challenge = challenge;
            this.answer = answer;
        }

        public String name() {
            return this.name;
        }

        public String challenge() {
            return this.challenge;
        }

        public String answer() {
            return this.answer;
        }

    }

    public static final class MultipleChoiceQuestion {

        private final String question;
        private final List<String> answers;
        private final String correctAnswer;

        public MultipleChoiceQuestion(String question, List<String> answers, String correctAnswer) {
            this.question = question;
            this.answers = answers;
            this.correctAnswer = correctAnswer;
        }

        public String question() {
            return this.question;
        }

        public List<String> answers() {
            return this.answers;
        }

        public String correctAnswer() {
            return this.correctAnswer;
        }

    }

}