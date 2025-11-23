package cz.muni.jena.test_data.god_di.lcom4;

@SuppressWarnings("all")
public class TestClassOneClusterMethodsCalling extends MiddleClass {

    private static final String STRING_1 = "Test string 1";
    private static final String STRING_2 = "Test string 2";

    String method01() {
        return STRING_1;
    }

    String method02() {

        final String string1 = method01();

        return string1 + STRING_2;
    }
}
