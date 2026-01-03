package cz.muni.fi.jena.delombok;

import cz.muni.fi.jena.AbstractJenaGradleTaskTest;
import cz.muni.fi.jena.JenaGradlePlugin;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JenaDelombokTaskTest extends AbstractJenaGradleTaskTest {

    private static String getBuildScript(@Nullable String delombokArguments) {
        String script = """
                plugins {
                    id 'java'
                    id 'jena-gradle-plugin'
                }
                repositories {
                    mavenCentral()
                }
                dependencies {
                    compileOnly 'org.projectlombok:lombok:1.18.30'
                }
                """;

        if (delombokArguments != null) {
            script += delombokArguments;
        }

        return script;
    }

    @Test
    void testDelombokExecution() throws IOException {
        String buildScript = getBuildScript(null);
        setupSources(buildScript);
        setupJavaSources();

        BuildResult result = runTask(JenaGradlePlugin.DELOMBOK_TASK);

        assertTaskOutcome(result, JenaGradlePlugin.DELOMBOK_TASK, TaskOutcome.SUCCESS);

        File outputDir = new File(testProjectDir, "src-delombok");
        assertTrue(outputDir.exists(), "Delombok output directory should exist");
        File outputFile = new File(outputDir, "/main/java/com/example/User.java");
        assertTrue(outputFile.exists(), "Delomboked file should exist");

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("public String getName()"));
        assertTrue(content.contains("public User()"));
        assertFalse(content.contains("@Data"));
    }

    private void setupJavaSources() throws IOException {
        Path sourceDir = testProjectDir.toPath().resolve("src/main/java/com/example");
        Files.createDirectories(sourceDir);
        Path sourceFile = sourceDir.resolve("User.java");

        String sourceContent =
                """
                        package com.example;
                        import lombok.Data;
                        @Data
                        public class User {
                            private String name;
                        }""";
        Files.writeString(sourceFile, sourceContent);
    }

    @Test
    void testLombokJarNotFound() throws IOException {
        String taskArguments = """
                %s {
                   lombokArtifact.artifactId = 'test'\s
                }
                """.formatted(JenaGradlePlugin.DELOMBOK_TASK);

        String buildScript = getBuildScript(taskArguments);
        setupSources(buildScript);
        setupJavaSources();

        BuildResult result = runTaskAndFail(JenaGradlePlugin.DELOMBOK_TASK);

        assertTaskOutcome(result, JenaGradlePlugin.DELOMBOK_TASK, TaskOutcome.FAILED);
        assertTrue(result.getOutput().contains("Lombok dependency not found in 'compileClasspath'. Delombok failed."));
    }
}
