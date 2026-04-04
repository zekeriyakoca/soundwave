package com.soundwave.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.HashSet;

public class DistinctElementsValidator implements ConstraintValidator<DistinctElements, Collection<?>> {

    @Override
    public boolean isValid(Collection<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return new HashSet<>(value).size() == value.size();
    }
}
