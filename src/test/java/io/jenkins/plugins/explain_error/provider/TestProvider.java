package io.jenkins.plugins.explain_error.provider;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class TestProvider extends OpenAIProvider {

    private boolean throwError = false;

    @DataBoundConstructor
    public TestProvider() {
        super("https://localhost:1234", "test-model", Secret.fromString("test-api-key"));
    }

    @Override
    public Assistant createAssistant() {
        return new Assistant() {
            @Override
            public String chat(String message) {
                if (throwError) {
                    throw new RuntimeException("Request failed.");
                }
                return "Request was successful";
            }
        };
    }

    public void setThrowError(boolean throwError) {
        this.throwError = throwError;
    }

    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Extension
    @Symbol("test")
    public static class DescriptorImpl extends BaseProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Test";
        }

        public String getDefaultModel() {
            return "test-model";
        }
    }
}
