package io.jenkins.plugins.explain_error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.jenkins.plugins.explain_error.provider.BaseAIProvider;
import io.jenkins.plugins.explain_error.provider.OllamaProvider;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

@WithJenkins
public class ConfigMigrationTest {

    @Test
    @LocalData
    void readOldData(JenkinsRule r) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        BaseAIProvider provider = config.getAiProvider();
        assertInstanceOf(OllamaProvider.class, provider);
        assertEquals("gemma3:1b", provider.getModel());
        assertEquals("http://localhost:11434", provider.getUrl());
    }
}
