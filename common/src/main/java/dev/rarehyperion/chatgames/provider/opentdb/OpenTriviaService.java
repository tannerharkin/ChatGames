package dev.rarehyperion.chatgames.provider.opentdb;

import dev.rarehyperion.chatgames.ChatGamesCore;
import dev.rarehyperion.chatgames.game.GameConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Central service managing Open Trivia DB API interactions.
 * Handles question fetching, caching, rate limiting, and session tokens.
 */
public final class OpenTriviaService {

    private static final String API_BASE_URL = "https://opentdb.com/api.php";
    private static final String TOKEN_URL = "https://opentdb.com/api_token.php?command=request";
    private static final String TOKEN_RESET_URL = "https://opentdb.com/api_token.php?command=reset&token=";
    private static final long RATE_LIMIT_MS = 5000; // 5 seconds between API calls
    private static final int BATCH_SIZE = 10;

    private final ChatGamesCore plugin;

    private final Queue<OpenTriviaQuestion> triviaCache;
    private final Queue<OpenTriviaQuestion> multipleChoiceCache;

    private String sessionToken;
    private long lastRequestTime;
    private boolean fetchingTrivia;
    private boolean fetchingMultipleChoice;
    private boolean initialized;

    public OpenTriviaService(final ChatGamesCore plugin) {
        this.plugin = plugin;
        this.triviaCache = new ConcurrentLinkedQueue<>();
        this.multipleChoiceCache = new ConcurrentLinkedQueue<>();
        this.lastRequestTime = 0;
        this.fetchingTrivia = false;
        this.fetchingMultipleChoice = false;
        this.initialized = false;
    }

    /**
     * Initialize the service - fetch session token.
     */
    public void initialize() {
        if (this.initialized) {
            return;
        }

        this.plugin.platform().getLogger().info("Initializing Open Trivia DB service...");
        this.initialized = true;

        // Fetch session token asynchronously
        this.plugin.platform().runTaskLater(this::fetchSessionToken, 20L);
    }

    /**
     * Get a trivia question from the cache, refilling if needed.
     *
     * @param settings The game's open trivia settings
     * @return An optional containing the question, or empty if unavailable
     */
    public Optional<GameConfig.QuestionAnswer> getTriviaQuestion(final GameConfig.OpenTriviaSettings settings) {
        if (!settings.isEnabled()) {
            return Optional.empty();
        }

        // Ensure service is initialized
        if (!this.initialized) {
            this.initialize();
        }

        final OpenTriviaQuestion question = this.triviaCache.poll();

        // Check if we need to refill
        if (this.triviaCache.size() < settings.getRefillThreshold()) {
            this.refillTriviaCache(settings);
        }

        if (question == null) {
            return Optional.empty();
        }

        final String trueFalsePrefix = this.plugin.configManager().getMessage("true-or-false-prefix", "True or false?");
        return Optional.of(question.toQuestionAnswer(trueFalsePrefix));
    }

    /**
     * Get a multiple choice question from the cache, refilling if needed.
     *
     * @param settings The game's open trivia settings
     * @return An optional containing the question, or empty if unavailable
     */
    public Optional<GameConfig.MultipleChoiceQuestion> getMultipleChoiceQuestion(final GameConfig.OpenTriviaSettings settings) {
        if (!settings.isEnabled()) {
            return Optional.empty();
        }

        // Ensure service is initialized
        if (!this.initialized) {
            this.initialize();
        }

        final OpenTriviaQuestion question = this.multipleChoiceCache.poll();

        // Check if we need to refill
        if (this.multipleChoiceCache.size() < settings.getRefillThreshold()) {
            this.refillMultipleChoiceCache(settings);
        }

        if (question == null) {
            return Optional.empty();
        }

        return Optional.of(question.toMultipleChoiceQuestion());
    }

    /**
     * Refill the trivia question cache asynchronously.
     *
     * @param settings The game's open trivia settings
     */
    public void refillTriviaCache(final GameConfig.OpenTriviaSettings settings) {
        if (this.fetchingTrivia || this.triviaCache.size() >= settings.getCacheSize()) {
            return;
        }

        this.fetchingTrivia = true;

        this.plugin.platform().runTaskLater(() -> {
            try {
                this.fetchQuestions(null, this.triviaCache, settings);
            } finally {
                this.fetchingTrivia = false;
            }
        }, calculateDelay());
    }

    /**
     * Refill the multiple choice question cache asynchronously.
     *
     * @param settings The game's open trivia settings
     */
    public void refillMultipleChoiceCache(final GameConfig.OpenTriviaSettings settings) {
        if (this.fetchingMultipleChoice || this.multipleChoiceCache.size() >= settings.getCacheSize()) {
            return;
        }

        this.fetchingMultipleChoice = true;

        this.plugin.platform().runTaskLater(() -> {
            try {
                this.fetchQuestions("multiple", this.multipleChoiceCache, settings);
            } finally {
                this.fetchingMultipleChoice = false;
            }
        }, calculateDelay());
    }

    private long calculateDelay() {
        final long now = System.currentTimeMillis();
        final long timeSinceLastRequest = now - this.lastRequestTime;

        if (timeSinceLastRequest >= RATE_LIMIT_MS) {
            return 1L; // Minimal delay
        }

        // Convert remaining ms to ticks (50ms per tick)
        return (RATE_LIMIT_MS - timeSinceLastRequest) / 50 + 1;
    }

    private void fetchQuestions(final String type, final Queue<OpenTriviaQuestion> cache, final GameConfig.OpenTriviaSettings settings) {
        try {
            final String urlString = buildApiUrl(type, settings);
            final String response = makeHttpRequest(urlString);

            if (response == null) {
                this.plugin.platform().getLogger().warn("Failed to fetch questions from Open Trivia DB");
                return;
            }

            final OpenTriviaParser.ParseResult result = OpenTriviaParser.parse(response);

            switch (result.responseCode()) {
                case 0: // Success
                    // For trivia cache (type == null), filter out questions unsuitable for free-form answers
                    final boolean isTrivia = type == null;
                    int added = 0;
                    for (final OpenTriviaQuestion question : result.questions()) {
                        if (!isTrivia || question.isSuitableForTrivia(3)) {
                            cache.offer(question);
                            added++;
                        }
                    }
                    if (this.plugin.platform().getConfigValue("debug", Boolean.class, false)) {
                        final String typeDesc = isTrivia ? "mixed" : type;
                        if (isTrivia && added < result.questions().size()) {
                            this.plugin.platform().getLogger().info("Fetched " + added + "/" + result.questions().size() + " " + typeDesc + " questions from Open Trivia DB (filtered unsuitable)");
                        } else {
                            this.plugin.platform().getLogger().info("Fetched " + added + " " + typeDesc + " questions from Open Trivia DB");
                        }
                    }
                    break;
                case 3: // Token not found
                    this.plugin.platform().getLogger().warn("Open Trivia DB token not found, requesting new token");
                    this.fetchSessionToken();
                    break;
                case 4: // Token exhausted
                    this.plugin.platform().getLogger().info("Open Trivia DB token exhausted, resetting token");
                    this.resetSessionToken();
                    break;
                default:
                    this.plugin.platform().getLogger().warn("Open Trivia DB API returned code: " + result.responseCode());
            }

            this.lastRequestTime = System.currentTimeMillis();

        } catch (final Exception e) {
            this.plugin.platform().getLogger().error("Error fetching questions from Open Trivia DB", e);
        }
    }

    private String buildApiUrl(final String type, final GameConfig.OpenTriviaSettings settings) {
        final StringBuilder url = new StringBuilder(API_BASE_URL);
        url.append("?amount=").append(BATCH_SIZE);
        if (type != null) {
            url.append("&type=").append(type);
        }
        url.append("&encode=base64");

        final List<Integer> categories = settings.getCategories();
        if (!categories.isEmpty()) {
            // Use a random category from the configured list
            final int categoryIndex = (int) (Math.random() * categories.size());
            url.append("&category=").append(categories.get(categoryIndex));
        }

        final String difficulty = settings.getDifficulty();
        if (difficulty != null && !difficulty.isEmpty()) {
            url.append("&difficulty=").append(difficulty);
        }

        if (this.sessionToken != null && !this.sessionToken.isEmpty()) {
            url.append("&token=").append(this.sessionToken);
        }

        return url.toString();
    }

    private void fetchSessionToken() {
        try {
            final String response = makeHttpRequest(TOKEN_URL);
            if (response == null) return;

            // Simple parse for token value
            final int tokenStart = response.indexOf("\"token\"");
            if (tokenStart == -1) return;

            final int colonPos = response.indexOf(':', tokenStart);
            if (colonPos == -1) return;

            final int quoteStart = response.indexOf('"', colonPos);
            if (quoteStart == -1) return;

            final int quoteEnd = response.indexOf('"', quoteStart + 1);
            if (quoteEnd == -1) return;

            this.sessionToken = response.substring(quoteStart + 1, quoteEnd);

            if (this.plugin.platform().getConfigValue("debug", Boolean.class, false)) {
                this.plugin.platform().getLogger().info("Obtained Open Trivia DB session token");
            }

        } catch (final Exception e) {
            this.plugin.platform().getLogger().error("Error fetching Open Trivia DB session token", e);
        }
    }

    private void resetSessionToken() {
        if (this.sessionToken == null || this.sessionToken.isEmpty()) {
            this.fetchSessionToken();
            return;
        }

        try {
            final String response = makeHttpRequest(TOKEN_RESET_URL + this.sessionToken);
            if (response != null && response.contains("\"response_code\":0")) {
                if (this.plugin.platform().getConfigValue("debug", Boolean.class, false)) {
                    this.plugin.platform().getLogger().info("Reset Open Trivia DB session token");
                }
            }
        } catch (final Exception e) {
            this.plugin.platform().getLogger().error("Error resetting Open Trivia DB session token", e);
            // Try getting a new token instead
            this.fetchSessionToken();
        }
    }

    private String makeHttpRequest(final String urlString) {
        HttpURLConnection connection = null;
        try {
            final URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "ChatGames/" + this.plugin.platform().pluginMeta().getVersion());

            final int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                final StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }

        } catch (final Exception e) {
            this.plugin.platform().getLogger().warn("HTTP request failed: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
