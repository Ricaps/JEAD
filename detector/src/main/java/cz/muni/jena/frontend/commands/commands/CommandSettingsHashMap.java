package cz.muni.jena.frontend.commands.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class CommandSettingsHashMap extends HashMap<String, String> implements CommandSettingsMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandSettingsHashMap.class);
    private static final String SEPARATOR = "=";
    public static final String EMPTY_SIGN = "<empty>";

    public static CommandSettingsMap of(List<String> config) {
        final CommandSettingsMap settingsMap = new CommandSettingsHashMap();

        if (config.size() == 1 && config.get(0).equals(EMPTY_SIGN)) {
            return settingsMap;
        }

        config.forEach(input -> {
            String[] split = input.split(SEPARATOR);
            if (split.length != 2) {
                LOGGER.warn("Failed to parse settings {}", input);
                return;
            }

            String key = split[0].trim();
            String value = split[1].trim();

            if (key.isEmpty()) {
                LOGGER.error("Key must not be empty!");
                return;
            }

            if (value.isEmpty()) {
                LOGGER.error("Value must not be empty!");
                return;
            }

            settingsMap.put(key, value);
        });

        return settingsMap;
    }

    @Override
    public Optional<String> getAsString(String key) {
        return Optional.ofNullable(get(key));
    }

    @Override
    public Optional<Boolean> getAsBoolean(String key) {
        String stringValue = get(key);

        if (stringValue == null) {
            return Optional.empty();
        }

        if (stringValue.equals("true")) {
            return Optional.of(true);
        }

        if (stringValue.equals("false")) {
            return Optional.of(false);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Integer> getAsInteger(String key) {
        String stringValue = get(key);

        if (stringValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(stringValue));
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse string to integer {}", stringValue, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Long> getAsLong(String key) {
        String stringValue = get(key);

        if (stringValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(stringValue));
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse string to long {}", stringValue, e);
            return Optional.empty();
        }
    }
}
