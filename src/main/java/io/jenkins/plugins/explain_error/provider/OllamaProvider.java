package io.jenkins.plugins.explain_error.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class OllamaProvider extends BaseAIProvider {

    private static final Logger LOGGER = Logger.getLogger(OllamaProvider.class.getName());

    @DataBoundConstructor
    public OllamaProvider(String url, String model) {
        super(url, model);
    }

    @Override
    public Assistant createAssistant() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl(getUrl())
                .modelName(getModel())
                .temperature(0.3)
                .timeout(Duration.ofSeconds(180))
                .logRequests(LOGGER.isLoggable(Level.FINE))
                .logResponses(LOGGER.isLoggable(Level.FINE))
                .build();
        return AiServices.create(Assistant.class, model);
    }

    @Override
    public boolean isNotValid(@CheckForNull TaskListener listener) {
        if (listener != null) {
            if (Util.fixEmptyAndTrim(getUrl()) == null) {
                listener.getLogger().println("No url configured for Ollama.");
            } else if (Util.fixEmptyAndTrim(getModel()) == null) {
                listener.getLogger().println("No Model configured for Ollama.");
            }
        }
        return Util.fixEmptyAndTrim(getUrl()) == null ||
                Util.fixEmptyAndTrim(getModel()) == null;
    }

    @Extension
    @Symbol("ollama")
    public static class DescriptorImpl extends BaseProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Ollama";
        }

        public String getDefaultModel() {
            return "gemma3:1b";
        }

        @POST
        @SuppressWarnings("lgtm[jenkins/no-permission-check]")
        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (value == null || value.isBlank()) {
                return FormValidation.error("URL is required.");
            }
            return super.doCheckUrl(value);
        }

        /**
         * Method to test the AI API configuration.
         * This is called when the "Test Configuration" button is clicked.
         */
        @POST
        public FormValidation doTestConfiguration(@QueryParameter("url") String url,
                                                  @QueryParameter("model") String model) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

            try {
                OllamaProvider provider = new OllamaProvider(url, model);
                String testResponse = provider.explainError("Send 'Configuration test successful' to me.");

                if (testResponse != null && testResponse.contains("Configuration test successful")) {
                    return FormValidation.ok("Configuration test successful! API connection is working properly.");
                } else if (testResponse != null && testResponse.contains("AI API Error:")) {
                    return FormValidation.error("" + testResponse);
                } else if (testResponse != null && testResponse.contains("Failed to get explanation from AI service")) {
                    return FormValidation.error("" + testResponse);
                } else if (testResponse != null && testResponse.contains("Unable to create assistant")) {
                    return FormValidation.error("" + testResponse);
                } else {
                    return FormValidation.error("Connection failed: No valid response received from AI service.");
                }

            } catch (IOException e) {
                return FormValidation.error("Connection failed: " + e.getMessage() + ". Please check your API URL and network connection.");
            } catch (Exception e) {
                return FormValidation.error("Test failed: " + e.getMessage());
            }
        }
    }
}
