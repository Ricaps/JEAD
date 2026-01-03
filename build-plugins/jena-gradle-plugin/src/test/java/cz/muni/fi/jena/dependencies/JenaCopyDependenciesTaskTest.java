package cz.muni.fi.jena.dependencies;

import cz.muni.fi.jena.AbstractJenaGradleTaskTest;
import cz.muni.fi.jena.JenaGradlePlugin;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JenaCopyDependenciesTaskTest extends AbstractJenaGradleTaskTest {

    private static final String COMMONS_LANG_DEPENDENCY = "implementation 'org.apache.commons:commons-lang3:3.12.0'";
    private static final String COMMONS_IO_DEPENDENCY = "implementation 'commons-io:commons-io:2.11.0'";
    private static final String COMMONS_LANG_JAR = "commons-lang3-3.12.0.jar";
    private static final String COMMONS_IO_JAR = "commons-io-2.11.0.jar";

    @Test
    void testCopyDependenciesExecution() throws IOException {
        setupSources(createBuildScript(COMMONS_LANG_DEPENDENCY, null));

        BuildResult result = runTask(JenaGradlePlugin.COPY_DEPENDENCIES_TASK);

        assertTaskOutcome(result, JenaGradlePlugin.COPY_DEPENDENCIES_TASK, TaskOutcome.SUCCESS);

        File outputDir = new File(testProjectDir, "target/dependencies");
        assertTrue(outputDir.exists(), "Dependencies output directory should exist");

        assertDependencyExists(outputDir, COMMONS_LANG_JAR);
    }

    @Test
    void testCopyDependenciesCustomOutputDirectory() throws IOException {
        String extraConfig = """
                %s {
                    outputFolder = file('custom/deps')
                }
                """.formatted(JenaGradlePlugin.COPY_DEPENDENCIES_TASK);

        setupSources(createBuildScript(COMMONS_LANG_DEPENDENCY, extraConfig));

        BuildResult result = runTask(JenaGradlePlugin.COPY_DEPENDENCIES_TASK);

        assertTaskOutcome(result, JenaGradlePlugin.COPY_DEPENDENCIES_TASK, TaskOutcome.SUCCESS);

        File outputDir = new File(testProjectDir, "custom/deps");
        assertTrue(outputDir.exists(), "Custom dependencies output directory should exist");

        assertDependencyExists(outputDir, COMMONS_LANG_JAR);
    }

    @Test
    void testCopyMultipleDependencies() throws IOException {
        String dependencies = """
                %s
                %s
                """.formatted(COMMONS_LANG_DEPENDENCY, COMMONS_IO_DEPENDENCY);

        setupSources(createBuildScript(dependencies, null));

        BuildResult result = runTask(JenaGradlePlugin.COPY_DEPENDENCIES_TASK);

        assertTaskOutcome(result, JenaGradlePlugin.COPY_DEPENDENCIES_TASK, TaskOutcome.SUCCESS);

        File outputDir = new File(testProjectDir, "target/dependencies");
        assertTrue(outputDir.exists(), "Dependencies output directory should exist");

        assertDependencyExists(outputDir, COMMONS_LANG_JAR);
        assertDependencyExists(outputDir, COMMONS_IO_JAR);
    }

    private String createBuildScript(String dependencies, String extraConfig) {
        return """
                plugins {
                    id 'java'
                    id 'jena-gradle-plugin'
                }
                repositories {
                    mavenCentral()
                }
                dependencies {
                    %s
                }
                %s
                """.formatted(dependencies, extraConfig != null ? extraConfig : "");
    }

    private void assertDependencyExists(File outputDir, String fileName) {
        File dependencyFile = new File(outputDir, fileName);
        assertTrue(dependencyFile.exists(), "Dependency file " + fileName + " should exist in the output directory");
    }
}
