package dev.rarehyperion.chatgames.provider.opentdb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Handles JSON parsing and Base64 decoding for Open Trivia DB API responses.
 * Uses simple string parsing to avoid adding a JSON library dependency.
 */
public final class OpenTriviaParser {

    private OpenTriviaParser() {
        // Utility class
    }

    /**
     * Parse an API response from Open Trivia DB.
     *
     * @param json The JSON response string
     * @return A ParseResult containing the response code and parsed questions
     */
    public static ParseResult parse(final String json) {
        final int responseCode = parseResponseCode(json);
        final List<OpenTriviaQuestion> questions = new ArrayList<>();

        if (responseCode != 0) {
            return new ParseResult(responseCode, questions);
        }

        // Find the results array
        final int resultsStart = json.indexOf("\"results\"");
        if (resultsStart == -1) {
            return new ParseResult(responseCode, questions);
        }

        final int arrayStart = json.indexOf('[', resultsStart);
        final int arrayEnd = findMatchingBracket(json, arrayStart);

        if (arrayStart == -1 || arrayEnd == -1) {
            return new ParseResult(responseCode, questions);
        }

        // Parse each question object in the array
        String remaining = json.substring(arrayStart + 1, arrayEnd);
        int objectStart;

        while ((objectStart = remaining.indexOf('{')) != -1) {
            final int objectEnd = findMatchingBrace(remaining, objectStart);
            if (objectEnd == -1) break;

            final String questionJson = remaining.substring(objectStart, objectEnd + 1);
            final OpenTriviaQuestion question = parseQuestion(questionJson);

            if (question != null) {
                questions.add(question);
            }

            remaining = remaining.substring(objectEnd + 1);
        }

        return new ParseResult(responseCode, questions);
    }

    /**
     * Decode a Base64 encoded string.
     *
     * @param encoded The Base64 encoded string
     * @return The decoded string
     */
    public static String decodeBase64(final String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return encoded;
        }
        try {
            final byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            // Not valid Base64, return as-is
            return encoded;
        }
    }

    private static int parseResponseCode(final String json) {
        final int codeStart = json.indexOf("\"response_code\"");
        if (codeStart == -1) return -1;

        final int colonPos = json.indexOf(':', codeStart);
        if (colonPos == -1) return -1;

        final StringBuilder numBuilder = new StringBuilder();
        for (int i = colonPos + 1; i < json.length(); i++) {
            final char c = json.charAt(i);
            if (Character.isDigit(c)) {
                numBuilder.append(c);
            } else if (numBuilder.length() > 0) {
                break;
            }
        }

        try {
            return Integer.parseInt(numBuilder.toString());
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    private static OpenTriviaQuestion parseQuestion(final String json) {
        final String category = decodeBase64(extractStringValue(json, "category"));
        final String difficulty = decodeBase64(extractStringValue(json, "difficulty"));
        final String question = decodeBase64(extractStringValue(json, "question"));
        final String correctAnswer = decodeBase64(extractStringValue(json, "correct_answer"));
        final List<String> incorrectAnswers = extractStringArray(json, "incorrect_answers");

        if (question == null || correctAnswer == null) {
            return null;
        }

        return new OpenTriviaQuestion(category, difficulty, question, correctAnswer, incorrectAnswers);
    }

    private static String extractStringValue(final String json, final String key) {
        final String searchKey = "\"" + key + "\"";
        final int keyStart = json.indexOf(searchKey);
        if (keyStart == -1) return null;

        final int colonPos = json.indexOf(':', keyStart);
        if (colonPos == -1) return null;

        // Find the opening quote of the value
        int valueStart = -1;
        for (int i = colonPos + 1; i < json.length(); i++) {
            final char c = json.charAt(i);
            if (c == '"') {
                valueStart = i + 1;
                break;
            } else if (!Character.isWhitespace(c)) {
                // Not a string value
                return null;
            }
        }

        if (valueStart == -1) return null;

        // Find the closing quote
        final int valueEnd = findClosingQuote(json, valueStart);
        if (valueEnd == -1) return null;

        return unescapeJson(json.substring(valueStart, valueEnd));
    }

    /**
     * Unescape JSON string escape sequences.
     *
     * @param str The JSON string value (without surrounding quotes)
     * @return The unescaped string
     */
    private static String unescapeJson(final String str) {
        if (str == null || !str.contains("\\")) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                final char next = str.charAt(i + 1);
                switch (next) {
                    case '"':
                    case '\\':
                    case '/':
                        result.append(next);
                        i++;
                        break;
                    case 'n':
                        result.append('\n');
                        i++;
                        break;
                    case 'r':
                        result.append('\r');
                        i++;
                        break;
                    case 't':
                        result.append('\t');
                        i++;
                        break;
                    case 'b':
                        result.append('\b');
                        i++;
                        break;
                    case 'f':
                        result.append('\f');
                        i++;
                        break;
                    default:
                        result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static List<String> extractStringArray(final String json, final String key) {
        final List<String> result = new ArrayList<>();
        final String searchKey = "\"" + key + "\"";
        final int keyStart = json.indexOf(searchKey);

        if (keyStart == -1) return result;

        final int arrayStart = json.indexOf('[', keyStart);
        final int arrayEnd = findMatchingBracket(json, arrayStart);

        if (arrayStart == -1 || arrayEnd == -1) return result;

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);
        int pos = 0;

        while (pos < arrayContent.length()) {
            final int quoteStart = arrayContent.indexOf('"', pos);
            if (quoteStart == -1) break;

            final int quoteEnd = findClosingQuote(arrayContent, quoteStart + 1);
            if (quoteEnd == -1) break;

            final String value = decodeBase64(unescapeJson(arrayContent.substring(quoteStart + 1, quoteEnd)));
            result.add(value);

            pos = quoteEnd + 1;
        }

        return result;
    }

    private static int findClosingQuote(final String str, final int start) {
        for (int i = start; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                i++; // Skip escaped character
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static int findMatchingBracket(final String str, final int start) {
        if (start == -1 || str.charAt(start) != '[') return -1;

        int depth = 1;
        boolean inString = false;

        for (int i = start + 1; i < str.length(); i++) {
            final char c = str.charAt(i);

            if (inString) {
                if (c == '\\' && i + 1 < str.length()) {
                    i++; // Skip escaped character
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static int findMatchingBrace(final String str, final int start) {
        if (start == -1 || str.charAt(start) != '{') return -1;

        int depth = 1;
        boolean inString = false;

        for (int i = start + 1; i < str.length(); i++) {
            final char c = str.charAt(i);

            if (inString) {
                if (c == '\\' && i + 1 < str.length()) {
                    i++; // Skip escaped character
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    /**
     * Result of parsing an Open Trivia DB API response.
     */
    public static final class ParseResult {

        private final int responseCode;
        private final List<OpenTriviaQuestion> questions;

        public ParseResult(final int responseCode, final List<OpenTriviaQuestion> questions) {
            this.responseCode = responseCode;
            this.questions = questions;
        }

        public int responseCode() {
            return this.responseCode;
        }

        public List<OpenTriviaQuestion> questions() {
            return this.questions;
        }

    }

}
