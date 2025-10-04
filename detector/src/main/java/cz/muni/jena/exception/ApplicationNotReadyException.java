package cz.muni.jena.exception;

public class ApplicationNotReadyException extends RuntimeException {
    public ApplicationNotReadyException(String message) {
        super(message);
    }
}
