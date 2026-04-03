package cz.muni.jena.frontend.commands.copy.config;

import cz.muni.jena.configuration.ConfigurationLoader;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.io.IOException;

@Command
public class CopyConfigCommand
{

    private static final String PATH_DESCRIPTION = "Path where the configuration should be exported";
    private static final String COPY_CONFIG_DESCRIPTION = "Export the default configuration file. After you export it then" +
            " you can edit it and use it's absolute path as a parameter for detectIssues command.";

    private final ConfigurationLoader configurationLoader;

    public CopyConfigCommand(ConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
    }

    @Command(command = "copyConfig", description = COPY_CONFIG_DESCRIPTION)
    public String copyConfig(
            @Option(longNames = "path", shortNames = 'p', required = true, description = PATH_DESCRIPTION) String path
    )
    {
        try
        {
            configurationLoader.copyConfiguration(path);
        } catch (IOException e)
        {
            return "Copying the default configuration file failed. Consider double checking if the path parameter is legal file name.";
        }
        return "Default configuration file has been copied to: " + path;
    }
}
