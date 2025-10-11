package cz.muni.jena.parser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningDetector;

import java.nio.file.Path;

public class MachineLearningDetectorCallback implements SourceRoot.Callback {

    private final MachineLearningDetector machineLearningDetector;
    private final Configuration configuration;

    public MachineLearningDetectorCallback(MachineLearningDetector machineLearningDetector, Configuration configuration) {
        this.machineLearningDetector = machineLearningDetector;
        this.configuration = configuration;
    }

    @Override
    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
        result.ifSuccessful(this::process);

        return Result.DONT_SAVE;
    }

    private void process(CompilationUnit compilationUnit) {
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
                .forEach(this::processNode);
    }

    private void processNode(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        machineLearningDetector.runDetector(classOrInterfaceDeclaration, configuration);
    }
}
