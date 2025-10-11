package cz.muni.jena.parser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;

import java.nio.file.Path;
import java.util.List;

public class CallbackCombiner implements SourceRoot.Callback {

    private final List<SourceRoot.Callback> callbackList;

    public CallbackCombiner(List<SourceRoot.Callback> callbackList) {
        this.callbackList = callbackList;
    }

    @Override
    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
        callbackList.forEach(callback -> callback.process(localPath, absolutePath, result));

        return Result.DONT_SAVE;
    }
}
