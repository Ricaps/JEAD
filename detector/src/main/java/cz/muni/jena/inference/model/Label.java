package cz.muni.jena.inference.model;

public record Label(String labelName, double value) {

    public boolean matches(String labelName, double value) {
        if (!this.labelName.equals(labelName)) {
            return false;
        }

        return !(value < this.value);
    }
}
