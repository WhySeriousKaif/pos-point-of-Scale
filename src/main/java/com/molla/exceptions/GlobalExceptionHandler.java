package com.molla.exceptions;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 🛡️ Global Exception Handler - Centralized error handling for all controllers
 * 
 * 👉 Purpose: Catch all exceptions and return consistent JSON error responses
 * 🔹 Why: Provides uniform error format across entire API (better frontend integration)
 * 
 * 📌 How it works:
 * - @RestControllerAdvice: Intercepts exceptions from all @RestController classes
 * - @ExceptionHandler: Maps specific exceptions to handler methods
 * - Returns: Consistent JSON with timestamp, error type, message, and HTTP status
 */
@Hidden  // 🔇 Hide from Swagger UI (not an API endpoint)
@RestControllerAdvice(basePackages = "com.molla.controllers")
public class GlobalExceptionHandler {

    /**
     * ❌ Handle NotFoundException (404)
     * 
     * 👉 Purpose: Return 404 when resource not found (e.g., "User not found")
     * 🔹 Example: GET /api/users/999 → 404 "User not found with id: 999"
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    /**
     * ⚠️ Handle BadRequestException (400)
     * 
     * 👉 Purpose: Return 400 for validation errors or business rule violations
     * 🔹 Example: "User already exists", "Cannot register as ADMIN"
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    /**
     * ⚠️ Handle UserException (400) - Legacy exception for backward compatibility
     * 
     * 👉 Purpose: Handle old UserException (maintained for compatibility)
     * 🔹 Note: New code should use BadRequestException or NotFoundException
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, Object>> handleUserException(UserException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    /**
     * 🚫 Handle AccessDeniedException (403) – e.g. non–super-admin calling GET /api/stores
     */
    /**
     * ✅ Handle Validation Errors (400)
     * 
     * 👉 Purpose: Handle @Valid annotation failures (e.g., missing required fields)
     * 🔹 Flow: Jakarta Validation fails → Spring throws MethodArgumentNotValidException → This handler catches it
     * 
     * 📌 Example: POST /auth/signup with empty email → 400 "Email is required"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // 📝 Get first validation error message
        String msg = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg);
    }

    /**
     * 💥 Handle Generic Exceptions (500)
     * 
     * 👉 Purpose: Catch-all for unexpected errors (database errors, null pointers, etc.)
     * 🔹 Why: Prevents exposing internal error details to clients (security)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", 
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
    }

    /**
     * 🏗️ Build Error Response - Helper method for consistent error format
     * 
     * 👉 Purpose: Create uniform JSON error response structure
     * 🔹 Response format:
     * {
     *   "timestamp": "2026-01-14T19:49:20.729Z",
     *   "error": "NOT_FOUND",
     *   "message": "User not found with id: 123",
     *   "status": 404
     * }
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());  // ⏰ When error occurred
        body.put("error", error);  // 🔖 Error type (NOT_FOUND, BAD_REQUEST, etc.)
        body.put("message", message);  // 📝 Human-readable error message
        body.put("status", status.value());  // 🔢 HTTP status code
        return ResponseEntity.status(status).body(body);
    }
}
