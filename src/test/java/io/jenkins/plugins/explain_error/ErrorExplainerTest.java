package io.jenkins.plugins.explain_error;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.explain_error.provider.OpenAIProvider;
import io.jenkins.plugins.explain_error.provider.TestProvider;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ErrorExplainerTest {

    @Test
    void testErrorExplainerBasicFunctionality(JenkinsRule jenkins) throws Exception {
        ErrorExplainer errorExplainer = new ErrorExplainer();
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        // Test when plugin is disabled
        config.setEnableExplanation(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        TaskListener listener = jenkins.createTaskListener();

        // Should not throw exception when disabled
        assertDoesNotThrow(() -> {
            errorExplainer.explainError(build, listener, "ERROR", 100);
        });
    }

    @Test
    void testErrorExplainerWithInvalidConfig(JenkinsRule jenkins) throws Exception {
        ErrorExplainer errorExplainer = new ErrorExplainer();
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        // Test with null API key
        config.setEnableExplanation(true);
        config.setAiProvider(new OpenAIProvider(null, "test-model", null));

        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        TaskListener listener = jenkins.createTaskListener();

        // Should not throw exception with null API key
        assertDoesNotThrow(() -> {
            errorExplainer.explainError(build, listener, "ERROR", 100);
        });
    }

    @Test
    void testErrorExplainerTextMethods(JenkinsRule jenkins) throws Exception {
        ErrorExplainer errorExplainer = new ErrorExplainer();
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        // Setup valid configuration
        config.setEnableExplanation(true);
        TestProvider provider = new TestProvider();
        config.setAiProvider(provider);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // Test with valid error text (will fail with API but should not throw exception)
        assertDoesNotThrow(() -> {
            String result = errorExplainer.explainErrorText("Build failed", build);
            assertEquals("Request was successful", result);
        });

        // Test with null input
        assertDoesNotThrow(() -> {
            String result = errorExplainer.explainErrorText(null, build);
            // Should return error message about no error text provided
            assertNotNull(result);
            assertEquals("No error text provided to explain.", result);
        });

        // Test with empty input
        assertDoesNotThrow(() -> {
            String result = errorExplainer.explainErrorText("", build);
            // Should return error message about no error text provided
            assertNotNull(result);
            assertEquals("No error text provided to explain.", result);
        });

        // Test with empty input
        assertDoesNotThrow(() -> {
            String result = errorExplainer.explainErrorText("   ", build);
            // Should return error message about no error text provided
            assertNotNull(result);
            assertEquals("No error text provided to explain.", result);
        });

        // Test with invalid config input
        assertDoesNotThrow(() -> {
            provider.setApiKey(null);
            String result = errorExplainer.explainErrorText("", build);
            // Should return error message about no error text provided
            assertNotNull(result);
            assertEquals("ERROR: Provider is not properly configured.", result);
        });

        // Test with request exception config input
        assertDoesNotThrow(() -> {
            provider.setApiKey(Secret.fromString("test-key"));
            provider.setThrowError(true);
            String result = errorExplainer.explainErrorText("Build failed", build);
            // Should return error message about no error text provided
            assertNotNull(result);
            assertEquals("Failed to communicate with AI service: Request failed.", result);
        });
    }
}
