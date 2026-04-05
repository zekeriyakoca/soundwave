package com.soundwave.domain.dto;

public class Result<T> {
    private T value;
    private Error error;

    // Needed for caching to be able to deserialize
    public Result() {
    }

    private Result(T value, Error error) {
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
