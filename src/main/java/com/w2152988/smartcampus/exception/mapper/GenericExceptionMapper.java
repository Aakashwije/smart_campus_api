package com.w2152988.smartcampus.exception.mapper;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.w2152988.smartcampus.model.ErrorResponse;

/**
 * Catches any exception not handled by the other mappers and returns
 * a 500 Internal Server Error with a JSON body.
 *
 * This is basically the safety net — without it, unhandled exceptions
 * would leak raw stack traces to the client, which is a security issue
 * (OWASP lists it under Security Misconfiguration). Stack traces expose
 * class names, file paths, library versions etc. that attackers can use.
 *
 * WebApplicationExceptions (like 404, 405) keep their original status
 * code but still get wrapped in our JSON error format.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

  @Override
  public Response toResponse(Throwable exception) {

    // WebApplicationException — keep the status code Jersey already picked
    if (exception instanceof WebApplicationException) {
      WebApplicationException wae = (WebApplicationException) exception;
      int status = wae.getResponse().getStatus();
      String reason = wae.getResponse().getStatusInfo().getReasonPhrase();

      // Only log at WARNING — these are expected routing/method errors, not bugs
      LOGGER.log(Level.WARNING, "WebApplicationException: {0} {1}",
          new Object[] { status, reason });

      ErrorResponse error = new ErrorResponse(
          reason,
          status,
          wae.getMessage() != null ? wae.getMessage() : reason,
          "A framework-level error occurred. Verify the request path, HTTP method, and Content-Type header.");

      return Response.status(status)
          .entity(error)
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    // Something we didn't expect — log it and return 500
    LOGGER.log(Level.SEVERE, "Unhandled exception caught by safety-net mapper", exception);

    ErrorResponse error = new ErrorResponse(
        "Internal Server Error",
        500,
        "An unexpected error occurred. Please contact the system administrator.",
        "This error has been logged. No further details are disclosed for security reasons.");
    // Don't expose the actual exception message or stack trace to the client

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(error)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
