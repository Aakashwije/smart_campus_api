package com.w2152988.smartcampus.exception.mapper;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.w2152988.smartcampus.exception.RoomNotEmptyException;
import com.w2152988.smartcampus.model.ErrorResponse;

/**
 * Returns a 409 Conflict response when a client tries to delete a room
 * that still has sensors assigned to it. I added this check to avoid
 * leaving sensors without a valid room reference.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

  private static final Logger LOGGER = Logger.getLogger(RoomNotEmptyExceptionMapper.class.getName());

  @Override
  public Response toResponse(RoomNotEmptyException exception) {
    LOGGER.log(Level.WARNING, "Room deletion blocked: {0}", exception.getMessage());
    ErrorResponse error = new ErrorResponse(
        "Conflict",
        409,
        exception.getMessage(),
        "The room is currently occupied by active hardware (sensors). "
            + "Please reassign or remove all sensors before deleting this room.");

    return Response.status(Response.Status.CONFLICT)
        .entity(error)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
