package io.jenkins.plugins.explain_error;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.explain_error.provider.BaseAIProvider;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Service class responsible for explaining errors using AI.
 */
public class ErrorExplainer {

    private static final Logger LOGGER = Logger.getLogger(ErrorExplainer.class.getName());

    public void explainError(Run<?, ?> run, TaskListener listener, String logPattern, int maxLines) {
        String jobInfo = run != null ? ("[" + run.getParent().getFullName() + " #" + run.getNumber() + "]") : "[unknown]";
        try {
            GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

            if (!config.isEnableExplanation()) {
                listener.getLogger().println("AI error explanation is disabled in global configuration.");
                return;
            }

            BaseAIProvider provider = config.getAiProvider();
            if (provider.isNotValid(listener)) {
                listener.getLogger()
                        .println("ERROR: The provider is not properly configured.");
                return;
            }

            // Extract error logs
            String errorLogs = extractErrorLogs(run, logPattern, maxLines);

            if (StringUtils.isBlank(errorLogs)) {
                listener.getLogger().println("No error logs found to explain.");
                return;
            }

            // Get AI explanation
            String explanation = provider.explainError(errorLogs);
            LOGGER.fine(jobInfo + " AI error explanation succeeded.");

            // Store explanation in build action
            ErrorExplanationAction action = new ErrorExplanationAction(explanation, errorLogs);
            run.addOrReplaceAction(action);

            // Explanation is now available on the job page, no need to clutter console output

        } catch (Exception e) {
            LOGGER.severe(jobInfo + " Failed to explain error: " + e.getMessage());
            listener.getLogger().println(jobInfo + " Failed to explain error: " + e.getMessage());
        }
    }

    private String extractErrorLogs(Run<?, ?> run, String logPattern, int maxLines) throws IOException {
        List<String> logLines = run.getLog(maxLines);

        if (StringUtils.isBlank(logPattern)) {
            // Return last few lines if no pattern specified
            return String.join("\n", logLines);
        }

        Pattern pattern = Pattern.compile(logPattern, Pattern.CASE_INSENSITIVE);
        StringBuilder errorLogs = new StringBuilder();

        for (String line : logLines) {
            if (pattern.matcher(line).find()) {
                errorLogs.append(line).append("\n");
            }
        }

        return errorLogs.toString();
    }

    /**
     * Explains error text directly without extracting from logs.
     * Used for console output error explanation.
     */
    public String explainErrorText(String errorText, Run<?, ?> run) {
        String jobInfo = run != null ? ("[" + run.getParent().getFullName() + " #" + run.getNumber() + "]") : "[unknown]";

        try {
            GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

            if (!config.isEnableExplanation()) {
                LOGGER.fine("AI error explanation is disabled in global configuration");
                return "AI error explanation is disabled in global configuration.";
            }

            BaseAIProvider provider = config.getAiProvider();
            if (provider.isNotValid(new LogTaskListener(LOGGER, Level.FINE))) {
                return "ERROR: Provider is not properly configured.";
            }

            if (StringUtils.isBlank(errorText)) {
                LOGGER.fine("No error text provided");
                return "No error text provided to explain.";
            }

            // Get AI explanation
            String explanation = provider.explainError(errorText);
            LOGGER.fine(jobInfo + " AI error explanation succeeded.");
            LOGGER.finer("Explanation length: " + (explanation != null ? explanation.length() : 0));

            return explanation;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, jobInfo + " Failed to explain error text: " + e.getMessage(), e);
            return "Failed to explain error: " + e.getMessage();
        }
    }
}
