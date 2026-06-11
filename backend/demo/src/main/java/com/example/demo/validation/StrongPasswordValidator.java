package com.example.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // Presence is enforced separately by @NotBlank.
            return true;
        }
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isUpperCase(current)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(current)) {
                hasLowercase = true;
            } else if (Character.isDigit(current)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(current)) {
                hasSymbol = true;
            }
        }
        return value.length() >= 8 && hasUppercase && hasLowercase && hasDigit && hasSymbol;
    }
}
