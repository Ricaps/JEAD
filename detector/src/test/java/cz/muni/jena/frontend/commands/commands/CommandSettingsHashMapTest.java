package cz.muni.jena.frontend.commands.commands;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandSettingsHashMapTest {

    @Test
    void testCreateMap_noValue_returnsEmpty() {
        var configMap = CommandSettingsHashMap.of(Collections.emptyList());

        assertThat(configMap).isEmpty();
    }

    @Test
    void testCreateMap_oneValue_returnsWithOneValue() {
        var configMap = CommandSettingsHashMap.of(List.of("key=value"));

        assertThat(configMap).hasSize(1);
        assertThat(configMap.getAsString("key")).isPresent().get().isEqualTo("value");
    }

    @Test
    void testCreateMap_onlyKeyProvided_isEmpty() {
        var configMap = CommandSettingsHashMap.of(List.of("key"));

        assertThat(configMap).isEmpty();
    }

    @Test
    void testCreateMap_onlyValueProvided_isEmpty() {
        var configMap = CommandSettingsHashMap.of(List.of("=value"));

        assertThat(configMap).isEmpty();
    }

    @Test
    void testCreateMap_onlySeparatorProvided_isEmpty() {
        var configMap = CommandSettingsHashMap.of(List.of("="));

        assertThat(configMap).isEmpty();
    }

    @Test
    void testCreateMap_getAsBoolean_correctValue() {
        var configMap = CommandSettingsHashMap.of(List.of("isTrue=true", "isFalse=false"));

        assertThat(configMap.getAsBoolean("isTrue")).isPresent().get().isEqualTo(true);
        assertThat(configMap.getAsBoolean("isFalse")).isPresent().get().isEqualTo(false);
    }

    @Test
    void testCreateMap_getAsBoolean_incorrectValue() {
        var configMap = CommandSettingsHashMap.of(List.of("isTrue=true1", "isFalse=2false"));

        assertThat(configMap.getAsBoolean("isTrue")).isEmpty();
        assertThat(configMap.getAsBoolean("isFalse")).isEmpty();
    }

    @Test
    void testCreateMap_getAsInteger_correctValue() {
        var configMap = CommandSettingsHashMap.of(List.of("isInteger=123456"));

        assertThat(configMap.getAsInteger("isInteger")).isPresent().get().isEqualTo(123456);
    }

    @Test
    void testCreateMap_getAsInteger_incorrectValue() {
        var configMap = CommandSettingsHashMap.of(List.of("isString=abc", "isLong=999999999999"));

        assertThat(configMap.getAsInteger("isString")).isEmpty();
        assertThat(configMap.getAsInteger("isLong")).isEmpty();
    }

    @Test
    void testCreateMap_getAsLong_correctValue() {
        var configMap = CommandSettingsHashMap.of(List.of("isLong=999999999999", "isInteger=123"));

        assertThat(configMap.getAsLong("isLong")).isPresent().get().isEqualTo(999999999999L);
        assertThat(configMap.getAsLong("isInteger")).isPresent().get().isEqualTo(123L);
    }

    @Test
    void testCreateMap_getAsLong_incorrectValue() {
        var configMap = CommandSettingsHashMap.of(List.of("isString=abc"));

        assertThat(configMap.getAsLong("isString")).isEmpty();
    }
}