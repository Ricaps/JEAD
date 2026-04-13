package cz.muni.jena.test_data.extractors;

@ServiceMarker
@SuppressWarnings("unused")
public class AnnotatedServiceFixture {

    public String publicOperation(@ServiceMarker String value) {
        return value;
    }

    String packageOperation() {
        return "ok";
    }

    private void privateOperation() {
    }
}



