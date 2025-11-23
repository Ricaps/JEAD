package cz.muni.jena.test_data.god_di.lcom5;

@SuppressWarnings("unused")
public class TestClassSomeMethodsUsesMoreFields extends BaseClass {
    private static final String FIELD = "text";
    private static final String FIELD_2 = "text";
    private static final String FIELD_3 = "text";

    public void method1() {
        System.out.println(FIELD);
        System.out.println(FIELD_2);
    }

    public void method2() {
        System.out.println(FIELD_2);
    }

    public void method3() {
        System.out.println(FIELD_3);
    }
}
