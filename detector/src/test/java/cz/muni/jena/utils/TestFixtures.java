package cz.muni.jena.utils;

import java.io.File;
import java.nio.file.Path;

public class TestFixtures {
    public static final String TEST_FIXTURES_MODULE = Path.of(System.getProperty("user.dir"))
            .getParent()
            .resolve("test-fixtures")
            .toAbsolutePath() + File.separator;
    public static final String AUTHORIZATION_SERVER_PROJECT = TEST_FIXTURES_MODULE + "authorization-server";
    public static final String ANTIPATTERNS_PROJECT = TEST_FIXTURES_MODULE + "antipatterns";
    public static final String POWER_MOCK_USAGE_PROJECT = TEST_FIXTURES_MODULE + "power-mock-usage";
    public static final String TEST_GRADLE_PROJECT = TEST_FIXTURES_MODULE + "test-gradle-project";
}
