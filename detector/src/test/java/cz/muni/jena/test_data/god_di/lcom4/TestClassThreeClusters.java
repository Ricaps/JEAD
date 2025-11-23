package cz.muni.jena.test_data.god_di.lcom4;

@SuppressWarnings("all")
public class TestClassThreeClusters extends MiddleClass {

    private static final String LOCAL_STRING = "test";

    public void method01() {
        baseClassString.contains("test");
    }

    public void method02() {
        middleClassString.contains("test2");
    }

    public void method03() {
        System.out.println(LOCAL_STRING);
    }
}
