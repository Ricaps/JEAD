package cz.muni.fi.jead;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractJeadGradleTaskTest {

    public static final String JAVA_11_COMPATIBLE_GRADLE = "8.14.3";
    @TempDir
    protected File testProjectDir;

    protected File buildFile;
    protected File settingsFile;

    @BeforeEach
    void setup() {
        buildFile = new File(testProjectDir, "build.gradle");
        settingsFile = new File(testProjectDir, "settings.gradle");
    }

    protected void setupSources(String buildScript) throws IOException {
        Files.writeString(settingsFile.toPath(), "rootProject.name = 'test-project'");

        String withJavaVersion = "java {\n" +
                "    sourceCompatibility = JavaVersion.VERSION_11\n" +
                "    targetCompatibility = JavaVersion.VERSION_11\n" +
                "}\n";

        Files.writeString(buildFile.toPath(), buildScript + "\n" + withJavaVersion);
    }

    protected BuildResult runTask(String taskName) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withGradleVersion(JAVA_11_COMPATIBLE_GRADLE)
                .withArguments(taskName, "--stacktrace")
                .build();
    }
    
    protected BuildResult runTaskAndFail(String taskName) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withGradleVersion(JAVA_11_COMPATIBLE_GRADLE)
                .withPluginClasspath()
                .withArguments(taskName, "--stacktrace")
                .buildAndFail();
    }

    protected void assertTaskOutcome(BuildResult result, String taskName, TaskOutcome expectedOutcome) {
        BuildTask task = result.task(":" + taskName);
        assertNotNull(task);
        assertEquals(expectedOutcome, task.getOutcome());
    }
}
