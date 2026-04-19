package com.w2152988.smartcampus.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.w2152988.smartcampus.exception.LinkedResourceNotFoundException;
import com.w2152988.smartcampus.model.ErrorResponse;
import com.w2152988.smartcampus.model.Room;
import com.w2152988.smartcampus.model.Sensor;
import com.w2152988.smartcampus.storage.DataStore;

/**
 * Handles all sensor endpoints at /api/v1/sensors.
 *
 * The @Consumes(APPLICATION_JSON) annotation on POST means JAX-RS will
 * automatically reject requests with the wrong Content-Type and return
 * 415 before my code even runs.
 *
 * I used ?type= as a query parameter for filtering rather than putting
 * the type in the URL path. Query parameters are better for optional
 * filtering because you can omit them to get all sensors, and you could
 * add more filters later without changing the URL structure.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

  private final DataStore store = DataStore.getInstance();

  private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "MAINTENANCE", "OFFLINE");

  @Context
  private UriInfo uriInfo;

  // GET /api/v1/sensors - returns sensors with HATEOAS links (paginated with
  // ?page=0&size=20),
  // optionally filtered by ?type=
  @GET
  public Response getAllSensors(@QueryParam("type") String type,
      @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("20") int size) {
    // Guard against negative or zero values that would break subList
    if (page < 0)
      page = 0;
    if (size < 1)
      size = 20;

    List<Sensor> result = new ArrayList<>(store.getSensors().values());

    if (type != null && !type.isBlank()) {
      result = result.stream()
          .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
          .collect(Collectors.toList());
    }

    int from = Math.min(page * size, result.size());
    int to = Math.min(from + size, result.size());
    List<Sensor> paged = result.subList(from, to);

    // Wrap each sensor with HATEOAS links (self, readings, room, collection)
    URI base = uriInfo.getBaseUri();
    List<Map<String, Object>> wrapped = paged.stream()
        .map(s -> HateoasHelper.sensorResponse(s, base))
        .collect(Collectors.toList());
    return Response.ok(wrapped).build();
  }

  // POST /api/v1/sensors — creates a new sensor
  // The roomId in the body must point to a real room or we return 422
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createSensor(Sensor sensor) {
    if (sensor.getId() == null || sensor.getId().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest("Sensor ID is required."))
          .build();
    }

    if (sensor.getType() == null || sensor.getType().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest("Sensor type is required."))
          .build();
    }

    // Validate status against the known enum values
    if (sensor.getStatus() != null && !sensor.getStatus().isBlank()
        && !VALID_STATUSES.contains(sensor.getStatus().toUpperCase())) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest(
              "Invalid sensor status '" + sensor.getStatus()
                  + "'. Valid values are: ACTIVE, MAINTENANCE, OFFLINE."))
          .build();
    }

    // Reject invalid initial values (NaN, Infinity)
    if (Double.isNaN(sensor.getCurrentValue()) || Double.isInfinite(sensor.getCurrentValue())) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest("currentValue must be a finite number."))
          .build();
    }

    // Make sure the room they're linking to actually exists
    if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
      throw new LinkedResourceNotFoundException(
          "Sensor payload must include a valid 'roomId'.");
    }

    Room targetRoom = store.getRoom(sensor.getRoomId());
    if (targetRoom == null) {
      throw new LinkedResourceNotFoundException(
          "The roomId '" + sensor.getRoomId() + "' does not reference an existing room.");
    }

    // putIfAbsent so two requests can't both create the same sensor
    Sensor existing = store.addSensorIfAbsent(sensor);
    if (existing != null) {
      return Response.status(Response.Status.CONFLICT)
          .entity(ErrorResponse.conflict("A sensor with ID '" + sensor.getId() + "' already exists."))
          .build();
    }

    // Link sensor to its room
    targetRoom.addSensorId(sensor.getId());

    URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
    return Response.created(location)
        .entity(HateoasHelper.sensorResponse(sensor, uriInfo.getBaseUri()))
        .build();
  }

  // GET /api/v1/sensors/{sensorId} - returns a single sensor with HATEOAS links
  // or 404
  @GET
  @Path("/{sensorId}")
  public Response getSensor(@PathParam("sensorId") String sensorId) {
    Sensor sensor = store.getSensor(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Sensor '" + sensorId + "' not found."))
          .build();
    }
    return Response.ok(HateoasHelper.sensorResponse(sensor, uriInfo.getBaseUri())).build();
  }

  // DELETE /api/v1/sensors/{sensorId} - also removes it from its parent room
  @DELETE
  @Path("/{sensorId}")
  public Response deleteSensor(@PathParam("sensorId") String sensorId) {
    Sensor sensor = store.getSensor(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Sensor '" + sensorId + "' not found."))
          .build();
    }

    // Remove the sensor from its room's sensor list too
    Room room = store.getRoom(sensor.getRoomId());
    if (room != null) {
      room.removeSensorId(sensorId);
    }

    store.removeSensor(sensorId);
    return Response.noContent().build(); // 204 No Content
  }

  // PUT /api/v1/sensors/{sensorId} - updates sensor, handles room reassignment
  @PUT
  @Path("/{sensorId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updatedSensor) {
    Sensor existing = store.getSensor(sensorId);
    if (existing == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Sensor '" + sensorId + "' not found."))
          .build();
    }

    // If they're changing the roomId, make sure the new room exists
    if (updatedSensor.getRoomId() != null && !updatedSensor.getRoomId().isBlank()) {
      Room newRoom = store.getRoom(updatedSensor.getRoomId());
      if (newRoom == null) {
        throw new LinkedResourceNotFoundException(
            "The roomId '" + updatedSensor.getRoomId()
                + "' does not reference an existing room.");
      }

      // Room changed — move the sensor from the old room to the new one
      if (!updatedSensor.getRoomId().equals(existing.getRoomId())) {
        // Remove sensor from old room
        Room oldRoom = store.getRoom(existing.getRoomId());
        if (oldRoom != null) {
          oldRoom.removeSensorId(sensorId);
        }
        // Add sensor to new room
        newRoom.addSensorId(sensorId);
      }
    }

    updatedSensor.setId(sensorId);
    store.addSensor(updatedSensor);
    return Response.ok(HateoasHelper.sensorResponse(updatedSensor, uriInfo.getBaseUri())).build();
  }

  // Sub-resource locator for /sensors/{sensorId}/readings
  // No HTTP method annotation here - JAX-RS passes the request to
  // SensorReadingResource
  @Path("/{sensorId}/readings")
  public SensorReadingResource getReadingsSubResource(
      @PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId, uriInfo);
  }
}
