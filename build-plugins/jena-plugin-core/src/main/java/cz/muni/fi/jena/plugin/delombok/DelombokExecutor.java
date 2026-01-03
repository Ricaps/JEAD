package cz.muni.fi.jena.plugin.delombok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes the delombok process for a given source directory.
 * This class handles the construction of the delombok command,
 * execution of the process, and logging of its output.
 */
public class DelombokExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelombokExecutor.class);

    private final File sourceDirectoryFile;
    private final File lombokJar;
    private final String outputDirectorySuffix;

    /**
     * Constructs a new DelombokExecutor.
     *
     * @param sourceDirectoryFile The root directory containing the source files to be delomboked.
     * @param lombokJar The path to the lombok.jar file.
     * @param outputDirectorySuffix A suffix to be appended to the source directory name
     *                              to form the output directory name.
     */
    public DelombokExecutor(File sourceDirectoryFile, File lombokJar, String outputDirectorySuffix) {
        this.sourceDirectoryFile = sourceDirectoryFile;
        this.lombokJar = lombokJar;
        this.outputDirectorySuffix = outputDirectorySuffix;
    }

    /**
     * Executes the delombok process.
     *
     * @throws DelombokExecutorException If the source directory is invalid or if the delombok process fails.
     */
    public void execute() throws DelombokExecutorException {
        String sourceDirectory = sourceDirectoryFile.getAbsolutePath();
        if (!sourceDirectoryFile.isDirectory()) {
            throw new DelombokExecutorException(String.format("The 'sourceDirectory' property %s is not a directory!", sourceDirectory));
        }

        String outputDirectory = sourceDirectoryFile.getName() + "-" + outputDirectorySuffix;

        File workingDirectory = sourceDirectoryFile.getParentFile();
        List<String> commands = getProcessCommand(sourceDirectory, outputDirectory);

        LOGGER.info("Starting delombok with command {}", String.join(" ", commands));
        LOGGER.info("At working directory {}", workingDirectory);

        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(workingDirectory);

        runDelombok(processBuilder);

        LOGGER.info("Delombok output written to {}", workingDirectory.toPath().resolve(outputDirectory));
    }

    private static void runDelombok(ProcessBuilder processBuilder) throws DelombokExecutorException {
        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new DelombokExecutorException("Delombok failed with status code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new DelombokExecutorException("Failed to run delombok!", e);
        }
    }

    private List<String> getProcessCommand(String sourceDirectory, String outputDirectory) {
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        commands.add(lombokJar.getAbsolutePath());
        commands.add("delombok");
        commands.add(sourceDirectory);
        commands.add("-d");
        commands.add(outputDirectory);
        return commands;
    }
}
