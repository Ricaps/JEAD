package cz.muni.jena.inference;

import cz.muni.jena.utils.NonShellIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@NonShellIntegrationTest
public class MockupTestIT {

    @Test
    void test_canBeStarted() {
        assertThat(true).isTrue();
    }
}
