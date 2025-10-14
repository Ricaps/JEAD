package cz.muni.jena.utils;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Testcontainers
@SpringBootTest(
        {
                "spring.shell.interactive.enabled=false",
                "spring.shell.script.enabled=false"
        }
)
public @interface NonShellIntegrationTest {
}
