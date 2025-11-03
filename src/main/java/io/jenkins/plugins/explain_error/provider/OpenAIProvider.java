package io.jenkins.plugins.explain_error.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class OpenAIProvider extends BaseAIProvider {

    private static final Logger LOGGER = Logger.getLogger(OpenAIProvider.class.getName());

    protected Secret apiKey;

    @DataBoundConstructor
    public OpenAIProvider(String url, String model, Secret apiKey) {
        super(url, model);
        this.apiKey = apiKey;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @Override
    public Assistant createAssistant() {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl(Util.fixEmptyAndTrim(getUrl())) // Will use default if null
                .apiKey(getApiKey().getPlainText())
                .modelName(getModel())
                .temperature(0.3)
                .logRequests(LOGGER.isLoggable(Level.FINE))
                .logResponses(LOGGER.isLoggable(Level.FINE))
                .build();

        return AiServices.create(Assistant.class, model);
    }

    @Override
    public boolean isNotValid(@CheckForNull TaskListener listener) {
        if (listener != null) {
            if (Util.fixEmptyAndTrim(Secret.toString(getApiKey())) == null) {
                listener.getLogger().println("No Api key configured for OpenAI.");
            } else if (Util.fixEmptyAndTrim(getModel()) == null) {
                listener.getLogger().println("No Model configured for OpenAI.");
            }
        }
        return Util.fixEmptyAndTrim(Secret.toString(getApiKey())) == null ||
                Util.fixEmptyAndTrim(getModel()) == null;
    }

    @Extension
    @Symbol("openai")
    public static class DescriptorImpl extends BaseProviderDescriptor {

        private static final String[] MODELS = new String[]{
                "gpt-5",
                "gpt-5-mini",
                "gpt-5-nano",
                "gpt-5-pro",
                "gpt-4.1",
                "gpt-4.1-mini",
                "gpt-4.1-nano",
                "gpt-4-turbo",
                "gpt-3.5-turbo"
        };

        @NonNull
        @Override
        public String getDisplayName() {
            return "OpenAI";
        }

        public String getDefaultModel() {
            return "gpt-4";
        }

        @POST
        @SuppressWarnings("lgtm[jenkins/no-permission-check]")
        public AutoCompletionCandidates doAutoCompleteModel(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            for (String model : MODELS) {
                if (model.toLowerCase().startsWith(value.toLowerCase())) {
                    c.add(model);
                }
            }
            return c;
        }

        /**
         * Method to test the AI API configuration.
         * This is called when the "Test Configuration" button is clicked.
         */
        @POST
        public FormValidation doTestConfiguration(@QueryParameter("apiKey") Secret apiKey,
                                                  @QueryParameter("url") String url,
                                                  @QueryParameter("model") String model) {
            Jenkins.get().checkPermission(Jenkins.MANAGE);


            try {
                OpenAIProvider provider = new OpenAIProvider(url, model, apiKey);
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
