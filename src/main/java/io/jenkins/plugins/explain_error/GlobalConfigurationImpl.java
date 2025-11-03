package io.jenkins.plugins.explain_error;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.explain_error.provider.BaseAIProvider;
import io.jenkins.plugins.explain_error.provider.GeminiProvider;
import io.jenkins.plugins.explain_error.provider.OllamaProvider;
import io.jenkins.plugins.explain_error.provider.OpenAIProvider;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.jenkinsci.Symbol;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Global configuration for the plugin.
 */
@Extension
@Symbol("explainError")
public class GlobalConfigurationImpl extends GlobalConfiguration {

    private transient Secret apiKey;
    private transient AIProvider provider;
    private transient String apiUrl;
    private transient String model;
    private boolean enableExplanation = true;

    private BaseAIProvider aiProvider;

    public GlobalConfigurationImpl() {
        load();
    }

    /**
     * Get the singleton instance of GlobalConfigurationImpl.
     * @return the GlobalConfigurationImpl instance
     */
    public static GlobalConfigurationImpl get() {
        return Jenkins.get().getDescriptorByType(GlobalConfigurationImpl.class);
    }

    public Object readResolve() {
        if (aiProvider == null && provider != null) {
            aiProvider = switch (provider) {
                case OPENAI -> new OpenAIProvider(apiUrl, model, apiKey);
                case GEMINI -> new GeminiProvider(apiUrl, model, apiKey);
                case OLLAMA -> new OllamaProvider(apiUrl, model);
            };
            save();
        }
        return this;
    }

    // Getters and setters
    public BaseAIProvider getAiProvider() {
        if (aiProvider == null) {
            readResolve();
        }
        return aiProvider;
    }

    public void setAiProvider(BaseAIProvider aiProvider) {
        this.aiProvider = aiProvider;
        save();
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    public AIProvider getProvider() {
        return provider;
    }

    @DataBoundSetter
    public void setProvider(AIProvider provider) {
        this.provider = provider;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    /**
     * Get the raw configured model without defaults, used for validation.
     */
    public String getRawModel() {
        return model;
    }

    @DataBoundSetter
    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEnableExplanation() {
        return enableExplanation;
    }

    @DataBoundSetter
    public void setEnableExplanation(boolean enableExplanation) {
        this.enableExplanation = enableExplanation;
    }

    @Override
    public String getDisplayName() {
        return "Explain Error Plugin Configuration";
    }

    @NonNull
    @Override
    public Permission getRequiredGlobalConfigPagePermission() {
        return Jenkins.MANAGE;
    }
}
