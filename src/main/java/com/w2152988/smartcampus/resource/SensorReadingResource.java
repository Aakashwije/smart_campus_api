package com.w2152988.smartcampus.resource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.w2152988.smartcampus.exception.SensorUnavailableException;
import com.w2152988.smartcampus.model.ErrorResponse;
import com.w2152988.smartcampus.model.Sensor;
import com.w2152988.smartcampus.model.SensorReading;
import com.w2152988.smartcampus.storage.DataStore;

/**
 * Sub-resource for sensor readings — not a top-level resource.
 * SensorResource's locator method creates an instance of this and JAX-RS
 * dispatches here. I separated readings out to keep SensorResource
 * focused on just sensor CRUD.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

  private final String sensorId;
  private final DataStore store = DataStore.getInstance();
  private final UriInfo uriInfo;

  public SensorReadingResource(String sensorId, UriInfo uriInfo) {
    this.sensorId = sensorId;
    this.uriInfo = uriInfo;
  }

  // GET /api/v1/sensors/{sensorId}/readings - returns reading history (paginated
  // with ?page=0&size=50)
  @GET
  public Response getReadings(@QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("50") int size) {
    Sensor sensor = store.getSensor(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Sensor '" + sensorId + "' not found."))
          .build();
    }

    // Guard against negative or zero values
    if (page < 0)
      page = 0;
    if (size < 1)
      size = 50;

    List<SensorReading> history = store.getReadings(sensorId);
    int from = Math.min(page * size, history.size());
    int to = Math.min(from + size, history.size());
    List<SensorReading> paged = history.subList(from, to);

    // Wrap each reading with HATEOAS links (self, sensor, collection)
    URI base = uriInfo.getBaseUri();
    List<Map<String, Object>> wrapped = paged.stream()
        .map(r -> HateoasHelper.readingResponse(r, sensorId, base))
        .collect(Collectors.toList());
    return Response.ok(wrapped).build();
  }

  // POST /api/v1/sensors/{sensorId}/readings - adds a new reading
  // Also updates the parent sensor's currentValue as a side effect
  // Returns 403 if the sensor is in MAINTENANCE or OFFLINE status
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addReading(SensorReading reading) {
    Sensor sensor = store.getSensor(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Sensor '" + sensorId + "' not found."))
          .build();
    }

    // Can't post readings to sensors that are in MAINTENANCE or OFFLINE
    if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())
        || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
      throw new SensorUnavailableException(
          "Sensor '" + sensorId + "' is currently in '"
              + sensor.getStatus() + "' state and cannot accept new readings.");
    }

    // Reject invalid reading values (NaN, Infinity)
    if (Double.isNaN(reading.getValue()) || Double.isInfinite(reading.getValue())) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest(
              "Reading value must be a finite number. NaN and Infinity are not accepted."))
          .build();
    }

    // Auto-generate an ID if the client didn't send one
    if (reading.getId() == null || reading.getId().isBlank()) {
      reading.setId(UUID.randomUUID().toString());
    }

    // Default timestamp to now if not provided
    if (reading.getTimestamp() <= 0) {
      reading.setTimestamp(System.currentTimeMillis());
    }

    // Persist the reading
    store.addReading(sensorId, reading);

    // Also update the sensor's currentValue with this reading
    sensor.setCurrentValue(reading.getValue());

    URI location = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
    return Response.created(location)
        .entity(HateoasHelper.readingResponse(reading, sensorId, uriInfo.getBaseUri()))
        .build();
  }

  // GET /api/v1/sensors/{sensorId}/readings/{readingId} - returns one reading
  @GET
  @Path("/{readingId}")
  public Response getReading(@PathParam("readingId") String readingId) {
    Sensor sensor = store.getSensor(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Sensor '" + sensorId + "' not found."))
          .build();
    }

    return store.getReadings(sensorId).stream()
        .filter(r -> r.getId().equals(readingId))
        .findFirst()
        .map(r -> Response.ok(HateoasHelper.readingResponse(r, sensorId, uriInfo.getBaseUri())).build())
        .orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(ErrorResponse.notFound(
                "Reading '" + readingId + "' not found for sensor '" + sensorId + "'."))
            .build());
  }
}
