package cz.muni.fi.jena.plugin.copy_dependencies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Executor class responsible for copying a collection of artifact files to a specified output folder.
 * It handles directory creation and robust file copying with logging.
 */
public class CopyDependenciesExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyDependenciesExecutor.class);
    private final Path outputFolder;
    private final Collection<File> artifacts;

    /**
     * Constructs a new {@code CopyDependenciesExecutor}.
     * @param outputFolder The target directory where dependencies will be copied.
     * @param artifacts A collection of {@link File} objects representing the dependencies to be copied.
     */
    public CopyDependenciesExecutor(Path outputFolder, Collection<File> artifacts) {
        this.outputFolder = outputFolder;
        this.artifacts = artifacts;
    }

    private static void createTargetDirectory(Path targetPath) throws CopyDependenciesException {
        try {
            Path createdDirectory = Files.createDirectories(targetPath);
            LOGGER.info("Created directory for dependencies {}", createdDirectory);
        } catch (IOException e) {
            throw new CopyDependenciesException("Failed to create target directory!", e);
        }
    }

    /**
     * Executes the dependency copying process.
     * It creates the target directory if it doesn't exist and then copies each artifact
     * to the output folder, replacing existing files if necessary.
     * @throws CopyDependenciesException If any error occurs during directory creation or file copying.
     */
    public void execute() throws CopyDependenciesException {
        if (!outputFolder.toFile().exists()) {
            createTargetDirectory(outputFolder);
        }

        AtomicInteger count = new AtomicInteger();
        for (File artifact : artifacts) {
            try {
                Path newPath = Files.copy(artifact.toPath(), outputFolder.resolve(artifact.getName()), StandardCopyOption.REPLACE_EXISTING);
                count.addAndGet(1);
                LOGGER.info("Copied dependency {} to path {}", artifact.getName(), newPath);
            } catch (IOException e) {
                throw new CopyDependenciesException("Failed to copy dependencies!", e);
            }
        }

        LOGGER.info("Copied {} dependencies to directory {}", count, outputFolder);
    }
}
