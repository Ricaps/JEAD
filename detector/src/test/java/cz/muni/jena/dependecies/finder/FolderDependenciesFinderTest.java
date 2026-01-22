package cz.muni.jena.dependecies.finder;

import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cz.muni.jena.Preconditions.verifyCorrectWorkingDirectory;
import static cz.muni.jena.utils.TestFixtures.ANTIPATTERNS_PROJECT;
import static cz.muni.jena.utils.TestFixtures.TEST_GRADLE_PROJECT;
import static org.assertj.core.api.Assertions.assertThat;

class FolderDependenciesFinderTest
{
    @Test
    void findJarTypeSolversInMavenProject()
    {
        verifyCorrectWorkingDirectory();
        List<JarTypeSolver> jarTypeSolvers = new FolderDependenciesFinder().findJarTypeSolvers(ANTIPATTERNS_PROJECT);
        assertThat(jarTypeSolvers).isNotEmpty();
    }

    @Test
    void findJarTypeSolversInGradleProject()
    {
        verifyCorrectWorkingDirectory();
        List<JarTypeSolver> jarTypeSolvers = new FolderDependenciesFinder().findJarTypeSolvers(TEST_GRADLE_PROJECT);
        assertThat(jarTypeSolvers).hasSize(2);
    }
}
