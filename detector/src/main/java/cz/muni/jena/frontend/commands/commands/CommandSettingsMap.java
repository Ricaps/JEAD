package cz.muni.jena.frontend.commands.commands;

import java.util.Map;
import java.util.Optional;

public interface CommandSettingsMap extends Map<String, String> {

    Optional<String> getAsString(String key);
    Optional<Boolean> getAsBoolean(String key);
    Optional<Integer> getAsInteger(String key);
    Optional<Long> getAsLong(String key);
}
