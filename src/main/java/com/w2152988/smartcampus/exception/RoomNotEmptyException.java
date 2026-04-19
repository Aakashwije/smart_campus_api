package com.w2152988.smartcampus.exception;

/**
 * Thrown when someone tries to delete a room that still has sensors
 * attached. Mapped to 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

  public RoomNotEmptyException(String message) {
    super(message);
  }
}
