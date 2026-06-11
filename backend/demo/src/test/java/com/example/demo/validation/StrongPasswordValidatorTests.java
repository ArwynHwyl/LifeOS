package com.example.demo.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrongPasswordValidatorTests {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsPasswordMeetingAllRules() {
        assertThat(validator.isValid("Admin12345!", null)).isTrue();
        assertThat(validator.isValid("aB3$aB3$", null)).isTrue();
        assertThat(validator.isValid("Sup3r-Secret", null)).isTrue();
    }

    @Test
    void rejectsPasswordsMissingARule() {
        assertThat(validator.isValid("Ab1!", null)).as("too short").isFalse();
        assertThat(validator.isValid("alllowercase1!", null)).as("no uppercase").isFalse();
        assertThat(validator.isValid("ALLUPPERCASE1!", null)).as("no lowercase").isFalse();
        assertThat(validator.isValid("NoNumbersHere!", null)).as("no digit").isFalse();
        assertThat(validator.isValid("NoSymbolHere1", null)).as("no symbol").isFalse();
        assertThat(validator.isValid("With Space 1!aA", null)).as("whitespace does not count as symbol").isTrue();
        assertThat(validator.isValid("With Space 1aA", null)).as("space alone is not a symbol").isFalse();
    }

    @Test
    void delegatesBlankToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("  ", null)).isTrue();
    }
}
