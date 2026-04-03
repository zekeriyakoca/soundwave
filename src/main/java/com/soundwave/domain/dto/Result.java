package com.soundwave.domain.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Result<T> {
    private final T value;
    private final Error error;

    // Needed for caching to be able to deserialize
    @JsonCreator
    private Result(@JsonProperty("value") T value, @JsonProperty("error") Error error) {
        this.value = value;
        this.error = error;
    }

    public T getValue() {
        return value;
    }

    public Error getError() {
        return error;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(Error error) {
        return new Result<>(null, error);
    }
}
