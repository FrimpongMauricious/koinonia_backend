// Canonical error shape returned by every non-2xx response.
// Consistent structure lets the React Native client handle errors generically.
package com.koinonia.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // omit `errors` when null (non-validation errors)
public class ApiError {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> errors; // populated only for 400 validation failures
}
