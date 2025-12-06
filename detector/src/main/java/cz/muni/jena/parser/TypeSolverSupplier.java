package cz.muni.jena.parser;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.dependecies.finder.FolderDependenciesFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TypeSolverSupplier implements Supplier<List<TypeSolver>>
{
    private final Path path;


    public TypeSolverSupplier(Path path)
    {
        this.path = path;
    }

    @Override
    public List<TypeSolver> get()
    {
        List<SourceRoot> sourceRoots =
                new SymbolSolverCollectionStrategy().collect(path).getSourceRoots();
        List<TypeSolver> currentTypeSolvers = new ArrayList<>();
        currentTypeSolvers.add(new ReflectionTypeSolver());
        sourceRoots.forEach(sr -> currentTypeSolvers.add(new JavaParserTypeSolver(sr.getRoot())));
        currentTypeSolvers.addAll(new FolderDependenciesFinder().findJarTypeSolvers(path.toString()));

        return currentTypeSolvers;
    }
}
