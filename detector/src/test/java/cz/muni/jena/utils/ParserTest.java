package cz.muni.jena.utils;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.Preconditions;
import cz.muni.jena.parser.TypeSolverSupplier;

import java.nio.file.Path;
import java.util.Optional;

public class ParserTest {

    public static final Path TEST_PACKAGE_PATH = Path.of(System.getProperty("user.dir"));

    private ParserTest() {
        super();
    }

    public static ClassOrInterfaceDeclaration getParsedClass(Class<?> clazz) {
        Preconditions.verifyCorrectWorkingDirectory();

        ProjectRoot projectRoot = getProjectRoot();

        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            System.out.println("Root: " + sourceRoot.getRoot());
            CompilationUnit compilationUnit = sourceRoot.parse(clazz.getPackageName(), clazz.getSimpleName() + ".java");
            Optional<ClassOrInterfaceDeclaration> classOptional = compilationUnit.getClassByName(clazz.getSimpleName());
            if (classOptional.isPresent()) {
                return classOptional.get();
            }
        }
        throw new IllegalStateException("Failed to find class %s".formatted(clazz.getSimpleName()));
    }

    private static ProjectRoot getProjectRoot() {
        System.out.println("Path: " + TEST_PACKAGE_PATH);
        TypeSolverSupplier typeSolverSupplier = new TypeSolverSupplier(TEST_PACKAGE_PATH);
        ParserConfiguration parserConfig = new ParserConfiguration();
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

        typeSolverSupplier.get().forEach(combinedTypeSolver::add);
        JavaSymbolSolver symbolResolver = new JavaSymbolSolver(combinedTypeSolver);
        parserConfig.setSymbolResolver(symbolResolver);
        return new SymbolSolverCollectionStrategy(parserConfig).collect(TEST_PACKAGE_PATH);
    }
}
