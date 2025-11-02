package io.jenkins.plugins.explain_error.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.util.Secret;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class GeminiProvider extends BaseAIProvider {

    private static final Logger LOGGER = Logger.getLogger(GeminiProvider.class.getName());

    private Secret apiKey;

    @DataBoundConstructor
    public GeminiProvider(String url, String model, Secret apiKey) {
        super(url, model);
        this.apiKey = apiKey;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @Override
    public Assistant createAssistant() {
        ChatModel model = GoogleAiGeminiChatModel.builder()
                .baseUrl(getUrl()) // Will use default if null
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
                listener.getLogger().println("No Api key configured for Gemini.");
            } else if (Util.fixEmptyAndTrim(getModel()) == null) {
                listener.getLogger().println("No Model configured for Gemini.");
            }
        }
        return Util.fixEmptyAndTrim(Secret.toString(getApiKey())) == null ||
                Util.fixEmptyAndTrim(getModel()) == null;
    }

    @Extension
    @Symbol("gemini")
    public static class DescriptorImpl extends BaseProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Google Gemini";
        }

        public String getDefaultModel() {
            return "gemini-2.0-flash";
        }
    }
}
