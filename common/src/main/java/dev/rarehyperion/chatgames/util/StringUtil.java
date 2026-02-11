package dev.rarehyperion.chatgames.util;

/**
 * Utility class for string operations including fuzzy matching.
 */
public final class StringUtil {

    private StringUtil() {
        // Utility class
    }

    /**
     * Calculate Levenshtein distance between two strings.
     * Returns the minimum number of single-character edits
     * (insertions, deletions, substitutions) to transform s1 into s2.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return The Levenshtein distance between the two strings
     */
    public static int levenshteinDistance(final String s1, final String s2) {
        final int len1 = s1.length();
        final int len2 = s2.length();

        // Use two rows instead of full matrix for space efficiency
        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];

        // Initialize first row
        for (int j = 0; j <= len2; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            curr[0] = i;

            for (int j = 1; j <= len2; j++) {
                final int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;

                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }

            // Swap rows
            final int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[len2];
    }

    /**
     * Check if two strings match within the given distance tolerance.
     *
     * @param input       The input string to check
     * @param target      The target string to match against
     * @param maxDistance Maximum allowed Levenshtein distance
     * @return true if the strings match within the tolerance
     */
    public static boolean fuzzyMatch(final String input, final String target, final int maxDistance) {
        if (input.equalsIgnoreCase(target)) {
            return true;
        }
        if (maxDistance <= 0) {
            return false;
        }
        return levenshteinDistance(input.toLowerCase(), target.toLowerCase()) <= maxDistance;
    }

    /**
     * Check if two strings match using word-based fuzzy matching.
     * The maximum allowed distance is calculated as: baseDistance + (wordCount * perWordDistance)
     * This is more generous for longer answers with multiple words.
     *
     * @param input           The input string to check
     * @param target          The target string to match against
     * @param baseDistance    The base number of allowed character differences
     * @param perWordDistance Additional allowed differences per word in the target
     * @return true if the strings match within the calculated tolerance
     */
    public static boolean fuzzyMatchByWords(final String input, final String target, final int baseDistance, final int perWordDistance) {
        if (input.equalsIgnoreCase(target)) {
            return true;
        }

        final int wordCount = countWords(target);
        final int maxDistance = baseDistance + (wordCount * perWordDistance);

        if (maxDistance <= 0) {
            return false;
        }

        return levenshteinDistance(input.toLowerCase(), target.toLowerCase()) <= maxDistance;
    }

    /**
     * Count the number of words in a string.
     * Words are separated by whitespace.
     *
     * @param str The string to count words in
     * @return The number of words
     */
    public static int countWords(final String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
        return str.trim().split("\\s+").length;
    }

    /**
     * Check if a string contains only numeric characters and optionally allowed characters.
     * Numeric characters are digits 0-9. Additional characters like '-' or '.' can be
     * specified as allowed.
     *
     * @param str          The string to check
     * @param allowedChars Additional characters to allow (e.g., '-', '.')
     * @return true if the string contains only digits and allowed characters
     */
    public static boolean isNumeric(final String str, final char... allowedChars) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (!Character.isDigit(c) && !isAllowedChar(c, allowedChars)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllowedChar(final char c, final char[] allowedChars) {
        for (final char allowed : allowedChars) {
            if (c == allowed) {
                return true;
            }
        }
        return false;
    }

}
