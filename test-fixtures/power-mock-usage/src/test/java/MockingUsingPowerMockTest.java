
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;

@PrepareForTest(ClassWithStaticMethod.class)
@RunWith(PowerMockRunner.class)
public class MockingUsingPowerMockTest
{
    @Test
    public void mockStaticMethodTest()
    {
        assertThat(ClassWithStaticMethod.helloWorld()).isEqualTo("Hello world!");
        PowerMockito.mockStatic(ClassWithStaticMethod.class);
        PowerMockito.when(ClassWithStaticMethod.helloWorld()).thenReturn("Fare well!");
        assertThat(ClassWithStaticMethod.helloWorld()).isEqualTo("Fare well!");

    }
}
