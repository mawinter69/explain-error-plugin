package io.jenkins.plugins.explain_error;

public class ExplanationException extends Exception {
    private final String level;
    public ExplanationException(String level, String message) {
        super(message);
        this.level = level;
    }

    public ExplanationException(String level, String message, Throwable cause) {
        super(message, cause);
        this.level = level;
    }

    public String getLevel() {
        return level;
    }
}
