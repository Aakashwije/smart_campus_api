package com.w2152988.smartcampus.model;

import java.time.Instant;

/**
 * Standard error response body used across the whole API.
 * Every error (from exception mappers or inline in resource methods)
 * uses this same shape so clients always know what to expect.
 *
 * Example JSON:
 * { "error": "Not Found", "status": 404, "message": "Room 'XYZ' not found.",
 * "detail": "...", "timestamp": "2026-04-03T10:15:30Z" }
 */
public class ErrorResponse {

  private String error;
  private int status;
  private String message;
  private String detail;
  private String timestamp;

  //  Constructors 

  public ErrorResponse() {
    this.timestamp = Instant.now().toString();
  }

  public ErrorResponse(String error, int status, String message, String detail) {
    this.error = error;
    this.status = status;
    this.message = message;
    this.detail = detail;
    this.timestamp = Instant.now().toString();
  }

  // Getters & Setters 

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  // Factory Methods 

  public static ErrorResponse notFound(String message) {
    return new ErrorResponse("Not Found", 404, message,
        "The requested resource does not exist on this server. "
            + "Verify the identifier and try again.");
  }

  public static ErrorResponse badRequest(String message) {
    return new ErrorResponse("Bad Request", 400, message,
        "The request was malformed or missing required fields. "
            + "Check the JSON payload structure.");
  }

  public static ErrorResponse conflict(String message) {
    return new ErrorResponse("Conflict", 409, message,
        "The request conflicts with the current state of the target resource.");
  }
}
