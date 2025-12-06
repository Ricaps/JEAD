package cz.muni.jena.frontend.commands.commands;

import jakarta.annotation.Nonnull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandSettingsListConverter implements Converter<List<String>, CommandSettingsMap> {

    @Override
    public CommandSettingsMap convert(@Nonnull List<String> source) {
        return CommandSettingsHashMap.of(source);
    }
}
