package com.soundwave.api.contract;

import com.soundwave.domain.dto.DomainErrorCode;
import com.soundwave.domain.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResultResponseMapper {

    private ResultResponseMapper() {
    }

    public static <T> ResponseEntity<?> toResponse(Result<T> result) {
        return toResponse(result, HttpStatus.OK);
    }

    public static <T> ResponseEntity<?> toResponse(Result<T> result, HttpStatus successStatus) {
        if (result.isSuccess()) {
            return ResponseEntity.status(successStatus).body(result.getValue());
        }
        var error = result.getError();
        return ResponseEntity.status(toStatus(error.code())).body(error);
    }

    private static HttpStatus toStatus(DomainErrorCode code) {
        return switch (code) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
        };
    }
}
