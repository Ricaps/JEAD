package cz.muni.jena.frontend.commands.commands;

import jakarta.annotation.Nonnull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandSettingsStringConverter implements Converter<String, CommandSettingsMap> {

    @Override
    public CommandSettingsMap convert(@Nonnull String source) {
        return CommandSettingsHashMap.of(List.of(source));
    }
}
