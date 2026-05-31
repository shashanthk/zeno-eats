package com.zenoeats.shared.util;

import com.zenoeats.shared.dto.ApiError;
import org.springframework.validation.BindingResult;

import java.util.List;

public final class ApiErrorUtils {

    private ApiErrorUtils() {
    }

    public static List<ApiError.FieldError> fromBindingResult(BindingResult br) {
        return br.getFieldErrors().stream()
            .map(fe -> ApiError.FieldError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .rejectedValue(fe.getRejectedValue())
                .build()
            )
            .toList();
    }
}
