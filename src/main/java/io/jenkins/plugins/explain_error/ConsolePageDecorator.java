package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.model.PageDecorator;

/**
 * Page decorator to add "Explain Error" functionality to console output pages.
 */
@Extension
public class ConsolePageDecorator extends PageDecorator {

    public ConsolePageDecorator() {
        super();
    }

    public boolean isExplainErrorEnabled() {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        // Must have explanation enabled. API key required for providers other than OLLAMA.
        if (!config.isEnableExplanation()) {
            return false;
        }

        return !config.getAiProvider().isNotValid(null);
    }
    
    /**
     * Helper method for JavaScript to check if a build is completed.
     * Returns true if the plugin is enabled (for JavaScript inclusion),
     * actual build status check is done in JavaScript.
     */
    public boolean isPluginActive() {
        return isExplainErrorEnabled();
    }
}
