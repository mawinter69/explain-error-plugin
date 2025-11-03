package io.jenkins.plugins.explain_error.provider;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public abstract class BaseAIProvider extends AbstractDescribableImpl<BaseAIProvider> implements ExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(BaseAIProvider.class.getName());

    protected String url;
    protected String model;

    public BaseAIProvider(String url, String model) {
        this.url = url;
        this.model = model;
    }

    public abstract Assistant createAssistant();

    public abstract boolean isNotValid(@CheckForNull TaskListener listener);

    public String getUrl() {
        return url;
    }

    public String getModel() {
        return model;
    }

    /**
     * Explain error logs using the configured AI provider.
     * @param errorLogs the error logs to explain
     * @return the AI explanation
     * @throws IOException if there's a communication error
     */
    public final String explainError(String errorLogs) throws IOException {
        Assistant assistant;

        if (StringUtils.isBlank(errorLogs)) {
            return "No error logs provided for explanation.";
        }

        if (isNotValid(null)) {
            return "Configuration is not valid.";
        }

        try {
            assistant = createAssistant();
        } catch (Exception e) {
            return "Unable to create assistant api-key or model is invalid.";
        }

        // Use PromptTemplate for dynamic prompt creation
        PromptTemplate promptTemplate = PromptTemplate.from(
                "You are an expert Jenkins administrator and software engineer. "
                        + "Please analyze the following Jenkins build error logs and provide a clear, "
                        + "actionable explanation of what went wrong and how to fix it:\n\n"
                        + "ERROR LOGS:\n"
                        + "{{errorLogs}}\n\n" + "Please provide:\n"
                        + "1. A summary of what caused the error\n"
                        + "2. Specific steps to resolve the issue\n"
                        + "3. Any relevant best practices to prevent similar issues\n\n"
                        + "Keep your response concise and focused on actionable solutions. "
                        + "Use plain text formatting only - no markdown, bold text, italic text, or special symbols for formatting."
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("errorLogs", errorLogs);
        Prompt prompt = promptTemplate.apply(variables);

        try {
            return assistant.chat(prompt.text());
        } catch (Exception e) {
            LOGGER.severe("AI API request failed: " + e.getMessage());
            return "Failed to communicate with AI service: " + e.getMessage();
        }
    }

    @Override
    public BaseProviderDescriptor getDescriptor() {
        return (BaseProviderDescriptor) super.getDescriptor();
    }

    public interface Assistant {
        String chat(String message);
    }

    public String getProviderName() {
        return getDescriptor().getDisplayName();
    }

    public abstract static class BaseProviderDescriptor extends Descriptor<BaseAIProvider> {
        public abstract String getDefaultModel();

        @POST
        @SuppressWarnings("lgtm[jenkins/no-permission-check]")
        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (value == null || value.isBlank()) {
                return FormValidation.ok();
            }
            try {
                URI uri = new URL(value).toURI();
                String scheme = uri.getScheme();
                if (uri.getHost() == null) {
                    return FormValidation.error("url is not well formed.");
                }
                if (!"http".equals(scheme) && !"https".equals(scheme)) {
                    return FormValidation.error("URL must use http or https");
                }
            } catch (MalformedURLException | URISyntaxException e) {
                return FormValidation.error(e, "URL is not well formed.");
            }
            return FormValidation.ok();
        }
    }
}
