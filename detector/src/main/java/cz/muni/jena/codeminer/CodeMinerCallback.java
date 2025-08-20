package cz.muni.jena.codeminer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;

import java.nio.file.Path;

public class CodeMinerCallback implements SourceRoot.Callback {

    private final CodeExtractor codeExtractor;
    private final OutputFormatter codeSerializer;

    public CodeMinerCallback(CodeExtractor codeExtractor, OutputFormatter codeSerializer) {
        this.codeExtractor = codeExtractor;
        this.codeSerializer = codeSerializer;
    }

    @Override
    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {

        result.ifSuccessful(this::processCompilationUnit);
        return Result.DONT_SAVE;
    }

    private void processCompilationUnit(CompilationUnit compilationUnit) {
        for (ClassOrInterfaceDeclaration declaration : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            codeExtractor.extract(declaration, codeSerializer);
        }
    }
}
