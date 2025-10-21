package cz.muni.jena.inference.model;

public record Label(String labelName, double value) {

    public boolean matches(String labelName, double referenceValue) {
        if (!this.labelName.equals(labelName)) {
            return false;
        }

        return this.value > referenceValue;
    }
}
