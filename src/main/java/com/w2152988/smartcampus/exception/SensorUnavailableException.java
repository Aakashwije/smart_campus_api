package com.w2152988.smartcampus.exception;

/**
 * Thrown when someone tries to post a reading to a sensor that's in
 * MAINTENANCE or OFFLINE mode. Mapped to 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {

  public SensorUnavailableException(String message) {
    super(message);
  }
}
