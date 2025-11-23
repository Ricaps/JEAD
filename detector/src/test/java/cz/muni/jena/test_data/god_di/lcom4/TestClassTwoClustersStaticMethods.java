package cz.muni.jena.test_data.god_di.lcom4;

@SuppressWarnings("all")
public class TestClassTwoClustersStaticMethods extends MiddleClass {

    private static final String STRING_1 = "test string";

    public static String method01() {
        return STRING_1;
    }

    public static String method2() {
        return MIDDLE_CLASS_STATIC;
    }
}
