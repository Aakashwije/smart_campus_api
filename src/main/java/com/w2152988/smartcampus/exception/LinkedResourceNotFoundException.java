package com.w2152988.smartcampus.exception;

/**
 * Thrown when a resource references another resource (like a sensor
 * pointing to a roomId) that doesn't exist. Mapped to 422 by its
 * exception mapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

  public LinkedResourceNotFoundException(String message) {
    super(message);
  }
}
