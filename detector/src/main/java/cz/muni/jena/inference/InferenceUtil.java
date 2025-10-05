package cz.muni.jena.inference;

import cz.muni.jena.exception.InferenceFailedException;

import java.util.UUID;

public class InferenceUtil {

    private InferenceUtil() {}

    public static UUID parseItemID(String itemID) {
        try {
            return UUID.fromString(itemID);
        } catch (IllegalArgumentException ex) {
            throw new InferenceFailedException("Failed to parse ID of the response item", ex);
        }

    }
}
