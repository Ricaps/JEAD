package cz.muni.jena.test_data.god_di.lcom4;

@SuppressWarnings("all")
public class TestClassOneCluster extends MiddleClass {

    private final String str = "some string";
    private final String str2 = "some string 2";

    private void method02(String param1, String param2) {
        System.out.println(str2);
        final boolean contains = str.contains("some");
    }

    public void method() {
        final String upperString = str2.toUpperCase();
        final String lowerString = str.toLowerCase();

        baseClassString.contains("abc");
    }

    public void method(String param1) {
        str2.toString();
        System.out.println(str);

        baseClassString.contains("abc");
    }
}
