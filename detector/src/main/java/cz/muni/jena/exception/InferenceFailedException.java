package cz.muni.jena.exception;

public class InferenceFailedException extends RuntimeException {

    public InferenceFailedException(String message) {
        super(message);
    }

    public InferenceFailedException(String message, Throwable exception) {
        super(message, exception);
    }
}
