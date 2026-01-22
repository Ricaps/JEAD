package cz.muni.antipatterns;

import com.example.antipatterns.AntiPatterns;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockingAntipatternsTest
{
    @Test
    void mockingStaticMethodTest()
    {
        assertThat(AntiPatterns.helloWorld()).isEqualTo("Hello world!");
        try (MockedStatic<AntiPatterns> utilities = Mockito.mockStatic(AntiPatterns.class)) {
            utilities.when(AntiPatterns::helloWorld).thenReturn("Fare well!");
            assertThat(AntiPatterns.helloWorld()).isEqualTo("Fare well!");
        }
    }

    @Test
    void mockingFinalMethodTest()
    {
        assertThat(AntiPatterns.helloWorld()).isEqualTo("Hello world!");
        ClassWithFinalMethod mock = mock(ClassWithFinalMethod.class);
        when(mock.helloWorld()).thenReturn("Fare well!");
        assertThat(mock.helloWorld()).isEqualTo("Fare well!");
    }

    @Test
    void mockingConstructorMethodTest()
    {
        assertThat(AntiPatterns.helloWorld()).isEqualTo("Hello world!");
        FinalClass mock = mock(FinalClass.class);
        when(mock.helloWorld()).thenReturn("Fare well!");
        assertThat(mock.helloWorld()).isEqualTo("Fare well!");
    }
}
