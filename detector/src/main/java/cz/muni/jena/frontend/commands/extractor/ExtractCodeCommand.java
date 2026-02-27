package cz.muni.jena.frontend.commands.extractor;

import cz.muni.jena.codeminer.CodeMinerCallback;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;
import cz.muni.jena.codeminer.outputformatter.OutputFormatterFactory;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.configuration.ConfigurationLoader;
import cz.muni.jena.frontend.commands.InvalidOptionException;
import cz.muni.jena.frontend.commands.commands.CommandSettingsHashMap;
import cz.muni.jena.frontend.commands.commands.CommandSettingsMap;
import cz.muni.jena.parser.AsyncCompilationUnitParser;
import org.springframework.shell.command.CommandExecution;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import javax.inject.Inject;
import java.util.List;

import static cz.muni.jena.codeminer.extractor.ExtractorUtils.getExtractorNames;

@Command
public class ExtractCodeCommand {

    private static final String EXTRACT_COMMAND_DESCRIPTION = "Extracts code from the given project with output to the given file and format";
    private static final String FORMAT_CMD_DESCRIPTION = "Choose the format of the output: json";
    private static final String PROJECT_PATH_CMD_DESCRIPTION = "Absolute path to project you wish to extract code";
    private static final String EXTRACTOR_CMD_DESCRIPTION = "Extractor defines what code snippets should be extracted from the project";
    private static final String OUTPUT_PATH_CMD_DESCRIPTION = "Path to the output file";
    private static final String PATH_TO_THE_CONFIGURATION_FILE = "Path to the configuration file";
    private static final String EXTRACTOR_SPECIFIC_SETTINGS_DESCRIPTION = "Enter settings specific to current extractor";
    private final OutputFormatterFactory outputFormatterFactory;
    private final List<CodeExtractor<?>> codeExtractorList;
    private final ConfigurationLoader configLoader;

    @Inject
    public ExtractCodeCommand(OutputFormatterFactory outputFormatterFactory, List<CodeExtractor<?>> codeExtractorList, ConfigurationLoader configLoader) {
        this.outputFormatterFactory = outputFormatterFactory;
        this.codeExtractorList = codeExtractorList;
        this.configLoader = configLoader;
    }

    @Command(command = "extractCode", description = EXTRACT_COMMAND_DESCRIPTION)
    public String extractCode(
            @Option(longNames = "configPath", shortNames = 'c', description = PATH_TO_THE_CONFIGURATION_FILE) String configPath,
            @Option(longNames = "projectPath", shortNames = 'p', required = true, description = PROJECT_PATH_CMD_DESCRIPTION) String projectPath,
            @Option(longNames = "format", shortNames = 'f', required = true, defaultValue = "json", description = FORMAT_CMD_DESCRIPTION) String format,
            @Option(longNames = "extractor", shortNames = 'e', required = true, description = EXTRACTOR_CMD_DESCRIPTION) String extractorName,
            @Option(longNames = "outputPath", shortNames = 'o', required = true, description = OUTPUT_PATH_CMD_DESCRIPTION) String outputPath,
            @Option(longNames = "settings", shortNames='s', description = EXTRACTOR_SPECIFIC_SETTINGS_DESCRIPTION, defaultValue = CommandSettingsHashMap.EMPTY_SIGN, arity = CommandRegistration.OptionArity.ONE_OR_MORE) CommandSettingsMap commandSettingsMap
            ) {

        try (OutputFormatter outputFormatter = outputFormatterFactory
                .getCodeSerializer(format)
                .orElseThrow(() -> new InvalidOptionException("Invalid output formatter."))) {
            outputFormatter.setOutputPath(outputPath);

            CodeExtractor<?> codeExtractor = codeExtractorList.stream()
                    .filter(extractor -> extractor.getIdentifier().equals(extractorName))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOptionException("Invalid code extractor. Possible values are: %s".formatted(getExtractorNames(codeExtractorList))));

            Configuration configuration = configLoader.loadConfig(configPath);

            CodeMinerCallback callback = new CodeMinerCallback(codeExtractor, outputFormatter, configuration, commandSettingsMap);
            AsyncCompilationUnitParser asyncCompilationUnitParser = new AsyncCompilationUnitParser(projectPath);

            asyncCompilationUnitParser.processCompilationUnits(callback);

            return "Code extracted successfully!";
        } catch (Exception e) {
            throw new CommandExecution.CommandExecutionException(e);
        }
    }
}
