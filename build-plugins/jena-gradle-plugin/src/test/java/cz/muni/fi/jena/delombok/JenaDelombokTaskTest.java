package cz.muni.fi.jena.delombok;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JenaDelombokTaskTest {

    @TempDir
    File testProjectDir;

    private File buildFile;
    private File settingsFile;

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

    @BeforeEach
    void setup() {
        buildFile = new File(testProjectDir, "build.gradle");
        settingsFile = new File(testProjectDir, "settings.gradle");
    }

    @Test
    void testDelombokExecution() throws IOException {
        String buildScript = getBuildScript(null);
        setupSources(buildScript);

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withArguments("delombok", "--stacktrace")
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":delombok").getOutcome());

        File outputDir = new File(testProjectDir, "src-delombok");
        assertTrue(outputDir.exists(), "Delombok output directory should exist");
        File outputFile = new File(outputDir, "/main/java/com/example/User.java");
        assertTrue(outputFile.exists(), "Delomboked file should exist");

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("public String getName()"));
        assertTrue(content.contains("public User()"));
        assertFalse(content.contains("@Data"));
    }

    private void setupSources(String buildScript) throws IOException {
        Files.writeString(settingsFile.toPath(), "rootProject.name = 'test-project'");

        Files.writeString(buildFile.toPath(), buildScript);

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
                delombok {
                   lombokArtifact.artifactId = 'test'\s
                }
                """;

        String buildScript = getBuildScript(taskArguments);
        setupSources(buildScript);

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withArguments("delombok", "--stacktrace")
                .buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":delombok").getOutcome());
        assertTrue(result.getOutput().contains("Lombok dependency not found in 'compileClasspath'. Delombok failed."));
    }
}