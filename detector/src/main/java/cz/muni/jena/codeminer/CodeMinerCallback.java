package cz.muni.jena.codeminer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;
import cz.muni.jena.configuration.Configuration;

import java.nio.file.Path;
import java.util.List;

public class CodeMinerCallback implements SourceRoot.Callback {

    private final CodeExtractor<?> codeExtractor;
    private final OutputFormatter codeSerializer;
    private final Configuration configuration;

    public CodeMinerCallback(CodeExtractor<?> codeExtractor, OutputFormatter codeSerializer, Configuration configuration) {
        this.codeExtractor = codeExtractor;
        this.codeSerializer = codeSerializer;
        this.configuration = configuration;
    }

    @Override
    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {

        result.ifSuccessful(this::processCompilationUnit);

        return Result.DONT_SAVE;
    }

    private void processCompilationUnit(CompilationUnit compilationUnit) {
        List<?> extractedCode = compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .flatMap(classOrIf -> codeExtractor.extract(classOrIf, configuration))
                .toList();

        codeSerializer.add(extractedCode);
    }
}
