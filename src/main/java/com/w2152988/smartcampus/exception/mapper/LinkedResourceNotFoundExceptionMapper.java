package com.w2152988.smartcampus.exception.mapper;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.w2152988.smartcampus.exception.LinkedResourceNotFoundException;
import com.w2152988.smartcampus.model.ErrorResponse;

/**
 * Returns a 422 Unprocessable Entity response when a sensor is created
 * with a roomId that does not exist. I chose 422 over 404 because the
 * endpoint itself is valid - the problem is that the roomId in the request
 * body points to a room that doesn't exist.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
    implements ExceptionMapper<LinkedResourceNotFoundException> {

  private static final Logger LOGGER = Logger.getLogger(LinkedResourceNotFoundExceptionMapper.class.getName());

  @Override
  public Response toResponse(LinkedResourceNotFoundException exception) {
    LOGGER.log(Level.WARNING, "Linked resource not found: {0}", exception.getMessage());
    ErrorResponse error = new ErrorResponse(
        "Unprocessable Entity",
        422,
        exception.getMessage(),
        "A foreign-key reference in the request body could not be resolved: "
            + exception.getMessage()
            + " Verify that all referenced identifiers (e.g. roomId) "
            + "correspond to existing entities before retrying.");

    return Response.status(422)
        .entity(error)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
