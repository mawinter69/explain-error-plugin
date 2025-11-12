package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.model.Run;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

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

    public String getProviderName() {
        return GlobalConfigurationImpl.get().getAiProvider().getProviderName();
    }

    /**
     * Helper method for JavaScript to check if a build is completed.
     * Returns true if the plugin is enabled (for JavaScript inclusion),
     * actual build status check is done in JavaScript.
     */
    public boolean isPluginActive() {
        String uri = Stapler.getCurrentRequest2().getRequestURI();
        return uri.matches(".*/console(Full)?$");
    }

    public String getRunUrl() {
        Ancestor ancestor = Stapler.getCurrentRequest2().findAncestor(Run.class);
        if (ancestor != null && ancestor.getObject() instanceof Run<?, ?> run) {
            return run.getUrl();
        } else {
            return null;
        }
    }

    public ErrorExplanationAction getExistingExplanation() {
        StaplerRequest2 request = Stapler.getCurrentRequest2();
        Ancestor ancestor = request.findAncestor(Run.class);
        if (ancestor != null && ancestor.getObject() instanceof Run<?, ?> run) {
            return run.getAction(ErrorExplanationAction.class);
        } else {
            return null;
        }
    }
}
