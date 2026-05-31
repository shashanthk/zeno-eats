package com.zenoeats.restauarantservice.exception;

import com.zenoeats.shared.dto.ApiError;
import com.zenoeats.shared.dto.ApiResponse;
import com.zenoeats.shared.dto.ErrorCode;
import com.zenoeats.shared.util.ApiErrorUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestaurantNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestaurantNotFound(RestaurantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        ApiError apiError = ApiError.builder()
            .code(ErrorCode.VALIDATION_FAILED.name())
            .detail("One or more fields failed validation")
            .fieldErrors(ApiErrorUtils.fromBindingResult(ex.getBindingResult()))
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Validation failed", apiError));
    }
}
