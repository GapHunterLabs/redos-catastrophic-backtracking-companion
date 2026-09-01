import java.util.regex.Pattern;

class Validator {

    // Flagged: nested unbounded quantifier -- classic ReDoS shape.
    private static final Pattern GREEDY_LINE = Pattern.compile("(a+)+$");

    // Flagged: overlapping alternation under a quantifier.
    private static final Pattern REDUNDANT_ALT = Pattern.compile("(a|a)*b");

    // Not flagged: bounded outer repeat.
    private static final Pattern BOUNDED = Pattern.compile("(a+){3}");

    // Not flagged: common, safe email-ish pattern.
    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    boolean isRisky(String input) {
        return input.matches("(a|a)*");
    }
}
