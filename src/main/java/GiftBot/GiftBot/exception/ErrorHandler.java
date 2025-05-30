package GiftBot.GiftBot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnknownError(final UnknownException e) {
        return ApiError.builder().message(e.getMessage())
                .reason(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                .errors(List.of(Arrays.toString(e.getStackTrace()))).status(HttpStatusCode.valueOf(500).toString())
                .timestamp(LocalDateTime.now().toString()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e) {
        return ApiError.builder().message(e.getMessage())
                .reason(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                .errors(List.of(Arrays.toString(e.getStackTrace()))).status(HttpStatusCode.valueOf(404).toString())
                .timestamp(LocalDateTime.now().toString()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(final ConflictException e) {
        return ApiError.builder().message(e.getMessage())
                .reason(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                .errors(List.of(Arrays.toString(e.getStackTrace()))).status(HttpStatusCode.valueOf(409).toString())
                .timestamp(LocalDateTime.now().toString()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbidden(final ForbiddenException e) {
        return ApiError.builder().message(e.getMessage())
                .reason(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                .errors(List.of(Arrays.toString(e.getStackTrace()))).status(HttpStatusCode.valueOf(403).toString())
                .timestamp(LocalDateTime.now().toString()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(final ValidationException e) {
        return ApiError.builder().message(e.getMessage())
                .reason(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                .errors(List.of(Arrays.toString(e.getStackTrace()))).status(HttpStatusCode.valueOf(400).toString())
                .timestamp(LocalDateTime.now().toString()).build();
    }
}
