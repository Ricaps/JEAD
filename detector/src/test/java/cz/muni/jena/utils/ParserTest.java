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

    public static final Path PROJECT_ROOT_PATH = Path.of(System.getProperty("user.dir"));
    public static final Path TEST_SOURCES_ROOT_PATH = PROJECT_ROOT_PATH.resolve("src/test/java").toAbsolutePath();

    private ParserTest() {
        super();
    }

    public static ClassOrInterfaceDeclaration getParsedClass(Class<?> clazz) {
        Preconditions.verifyCorrectWorkingDirectory();
        ProjectRoot projectRoot = getProjectRoot();

        SourceRoot sourceRoot = projectRoot
                .getSourceRoot(TEST_SOURCES_ROOT_PATH)
                .orElseThrow(() -> new IllegalStateException("Failed to find sources root for %s".formatted(TEST_SOURCES_ROOT_PATH)));
        CompilationUnit compilationUnit = sourceRoot.parse(clazz.getPackageName(), clazz.getSimpleName() + ".java");
        Optional<ClassOrInterfaceDeclaration> classOptional = compilationUnit.getClassByName(clazz.getSimpleName());
        if (classOptional.isPresent()) {
            return classOptional.get();
        }
        throw new IllegalStateException("Failed to find class %s".formatted(clazz.getSimpleName()));
    }

    private static ProjectRoot getProjectRoot() {
        TypeSolverSupplier typeSolverSupplier = new TypeSolverSupplier(PROJECT_ROOT_PATH);
        ParserConfiguration parserConfig = new ParserConfiguration();
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

        typeSolverSupplier.get().forEach(combinedTypeSolver::add);
        JavaSymbolSolver symbolResolver = new JavaSymbolSolver(combinedTypeSolver);
        parserConfig.setSymbolResolver(symbolResolver);
        return new SymbolSolverCollectionStrategy(parserConfig).collect(PROJECT_ROOT_PATH);
    }
}
