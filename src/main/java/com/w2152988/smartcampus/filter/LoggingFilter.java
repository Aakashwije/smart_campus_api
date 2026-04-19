package com.w2152988.smartcampus.filter;

import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * Logs every incoming request and outgoing response.
 * I implemented both ContainerRequestFilter and ContainerResponseFilter
 * in one class so that all logging is in one place rather than copied
 * into every resource method. It also logs the time taken to handle
 * each request.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

  // Passes the request start time through to the response filter
  private static final String START_TIME_PROPERTY = "com.w2152988.smartcampus.requestStartTime";

  // Called before the request reaches the resource method
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

    LOGGER.info(() -> String.format(">>> REQUEST  %s %s [Content-Type: %s]",
        requestContext.getMethod(),
        requestContext.getUriInfo().getRequestUri(),
        requestContext.getMediaType()));
  }

  // Called after the response is ready - logs the status code and time taken
  @Override
  public void filter(ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) throws IOException {

    // Null-guard: the start-time property may be absent if the request
    // filter was bypassed (e.g. Tomcat-level redirect or static resource)
    Object startObj = requestContext.getProperty(START_TIME_PROPERTY);
    long elapsed = (startObj != null) ? System.currentTimeMillis() - (long) startObj : -1;

    LOGGER.info(() -> String.format("<<< RESPONSE %s %s → %d %s [%d ms]",
        requestContext.getMethod(),
        requestContext.getUriInfo().getRequestUri(),
        responseContext.getStatus(),
        responseContext.getStatusInfo().getReasonPhrase(),
        elapsed));
  }
}
