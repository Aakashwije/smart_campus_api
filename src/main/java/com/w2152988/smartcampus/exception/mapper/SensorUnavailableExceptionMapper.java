package com.w2152988.smartcampus.exception.mapper;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.w2152988.smartcampus.exception.SensorUnavailableException;
import com.w2152988.smartcampus.model.ErrorResponse;

/**
 * Returns a 403 Forbidden response when a reading is posted to a sensor
 * that is in MAINTENANCE or OFFLINE status. The sensor exists but is
 * not currently accepting new data.
 */
@Provider
public class SensorUnavailableExceptionMapper
    implements ExceptionMapper<SensorUnavailableException> {

  private static final Logger LOGGER = Logger.getLogger(SensorUnavailableExceptionMapper.class.getName());

  @Override
  public Response toResponse(SensorUnavailableException exception) {
    LOGGER.log(Level.WARNING, "Sensor unavailable: {0}", exception.getMessage());
    ErrorResponse error = new ErrorResponse(
        "Forbidden",
        403,
        exception.getMessage(),
        "The target sensor is currently in a non-operational state "
            + "(e.g. MAINTENANCE or OFFLINE) and cannot accept new readings. "
            + "Restore the sensor to ACTIVE status before posting data.");

    return Response.status(Response.Status.FORBIDDEN)
        .entity(error)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
