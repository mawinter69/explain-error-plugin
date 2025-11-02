package io.jenkins.plugins.explain_error.provider;

import static org.junit.jupiter.api.Assertions.*;

import hudson.util.Secret;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ProviderTest {

    @Test
    void testExplainErrorWithNullInput() throws IOException {
        BaseAIProvider provider = new OpenAIProvider(null, "test-model", Secret.fromString("test-key"));
        String result = provider.explainError(null);
        assertEquals("No error logs provided for explanation.", result);
    }

    @Test
    void testExplainErrorWithEmptyInput() throws IOException {
        BaseAIProvider provider = new OpenAIProvider(null, "test-model", Secret.fromString("test-key"));
        String result = provider.explainError("");
        assertEquals("No error logs provided for explanation.", result);
    }

    @Test
    void testExplainErrorWithBlankInput() throws IOException {
        BaseAIProvider provider = new OpenAIProvider(null, "test-model", Secret.fromString("test-key"));
        String result = provider.explainError("   ");
        assertEquals("No error logs provided for explanation.", result);
    }

    @Test
    void testErrorLogsProcessing() throws IOException {
        BaseAIProvider provider = new TestProvider();
        String complexErrorLogs = "Started by user admin\n" +
                                 "Building in workspace /var/jenkins_home/workspace/test\n" +
                                 "ERROR: Could not find or load main class Application\n" +
                                 "FAILURE: Build failed with an exception.\n" +
                                 "* What went wrong:\n" +
                                 "Execution failed for task ':compileJava'.\n" +
                                 "> Compilation failed; see the compiler error output for details.\n" +
                                 "BUILD FAILED in 15s";

        String result = provider.explainError(complexErrorLogs);

        // Should not return the "no error logs" message for valid input
        assertEquals("Request was successful", result);
    }

    @Test
    void testErrorLogsProcessingFailure() throws IOException {
        TestProvider provider = new TestProvider();
        provider.setThrowError(true);
        String logs = "All is good.";

        String result = provider.explainError(logs);

        // Should return failure message
        assertEquals("Failed to communicate with AI service: Request failed.", result);
    }

    @Test
    void testOpenAIWithNullApiKey() {
        BaseAIProvider provider = new OpenAIProvider(null, "test-model", null);
        String result = assertDoesNotThrow(() -> provider.explainError("Some error"));

        // Should handle null API key gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testOpenAIWithEmptyApiKey() {
        BaseAIProvider provider = new OpenAIProvider(null, "test-model", Secret.fromString(""));
        String result = assertDoesNotThrow(() -> provider.explainError("Some error"));

        // Should handle empty API key gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testOpenAIWithNullModel() {
        BaseAIProvider provider = new OpenAIProvider(null, null, Secret.fromString("test-key"));
        String result = assertDoesNotThrow(() -> provider.explainError("Some error"));

        // Should handle null model gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testOpenAIWithEmptyModel() {
        BaseAIProvider provider = new OpenAIProvider(null, "", Secret.fromString("test-key"));
        String result = assertDoesNotThrow(() -> provider.explainError("Some error"));

        // Should handle empty model gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testGeminiNullApiKey() {
        BaseAIProvider provider = new GeminiProvider(null, "test-model", null);
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle null API key gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testGeminiEmptyApiKey() {
        BaseAIProvider provider = new GeminiProvider(null, "test-model", Secret.fromString(""));
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle empty API key gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testGeminiEmptyModel() {
        BaseAIProvider provider = new GeminiProvider(null, "", Secret.fromString("test-key"));
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle empty model gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testGeminiNullModel() {
        BaseAIProvider provider = new GeminiProvider(null, null, Secret.fromString("test-key"));
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle null model gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testOllamaNullModel() {
        BaseAIProvider provider = new OllamaProvider("http://localhost:1234", null);
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle null model gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testOllamaEmptyModel() {
        BaseAIProvider provider = new OllamaProvider("http://localhost:1234", "");
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle empty model gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testOllamaEmptyUrl() {
        BaseAIProvider provider = new OllamaProvider("", "test-model");
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle empty url gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }

    @Test
    void testOllamaNullUrl() {
        BaseAIProvider provider = new OllamaProvider(null, "test-model");
        String result = assertDoesNotThrow(() -> provider.explainError("Test error"));

        // Should handle null url gracefully - just verify we get some response
        assertEquals("Configuration is not valid.", result);
    }
}
