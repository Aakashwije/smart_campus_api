package com.w2152988.smartcampus.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import com.w2152988.smartcampus.exception.RoomNotEmptyException;
import com.w2152988.smartcampus.model.ErrorResponse;
import com.w2152988.smartcampus.model.Room;
import com.w2152988.smartcampus.storage.DataStore;

/**
 * Handles all room endpoints at /api/v1/rooms.
 *
 * I return the full room object in list responses rather than just IDs
 * so the client doesn't need to make extra requests for each room.
 * For a very large dataset pagination would be needed, but for this
 * project it keeps things simple.
 *
 * DELETE is idempotent - calling it on a room that is already gone
 * returns 404, which still means the room is absent, so the outcome
 * is the same regardless of how many times you call it.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

  private final DataStore store = DataStore.getInstance();

  @Context
  private UriInfo uriInfo;

  // GET /api/v1/rooms - returns rooms with HATEOAS links (paginated with
  // ?page=0&size=20)
  @GET
  public Response getAllRooms(@QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("20") int size) {
    // Guard against negative or zero values that would break subList
    if (page < 0)
      page = 0;
    if (size < 1)
      size = 20;

    List<Room> all = new ArrayList<>(store.getRooms().values());
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    List<Room> paged = all.subList(from, to);

    // Wrap each room with HATEOAS links so clients can navigate to
    // individual rooms, their sensors, etc. without hardcoding URLs
    URI base = uriInfo.getBaseUri();
    List<Map<String, Object>> wrapped = paged.stream()
        .map(r -> HateoasHelper.roomResponse(r, base))
        .collect(Collectors.toList());
    return Response.ok(wrapped).build();
  }

  // POST /api/v1/rooms - creates a new room, returns 201 with Location header
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createRoom(Room room) {
    if (room.getId() == null || room.getId().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest("Room ID is required."))
          .build();
    }

    if (room.getName() == null || room.getName().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest("Room name is required."))
          .build();
    }

    if (room.getCapacity() <= 0) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest("Room capacity must be greater than 0."))
          .build();
    }

    // putIfAbsent so two requests can't both create the same room
    Room existing = store.addRoomIfAbsent(room);
    if (existing != null) {
      return Response.status(Response.Status.CONFLICT)
          .entity(ErrorResponse.conflict("A room with ID '" + room.getId() + "' already exists."))
          .build();
    }

    URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
    return Response.created(location)
        .entity(HateoasHelper.roomResponse(room, uriInfo.getBaseUri()))
        .build();
  }

  // GET /api/v1/rooms/{roomId} - returns a single room with HATEOAS links or 404
  @GET
  @Path("/{roomId}")
  public Response getRoom(@PathParam("roomId") String roomId) {
    Room room = store.getRoom(roomId);
    if (room == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Room '" + roomId + "' not found."))
          .build();
    }
    return Response.ok(HateoasHelper.roomResponse(room, uriInfo.getBaseUri())).build();
  }

  // DELETE /api/v1/rooms/{roomId} — blocked with 409 if room still has sensors.
  // removeRoomIfEmpty() does the check-and-remove atomically so a sensor
  // can't sneak in between the check and the delete.
  @DELETE
  @Path("/{roomId}")
  public Response deleteRoom(@PathParam("roomId") String roomId) {
    Optional<Boolean> result = store.removeRoomIfEmpty(roomId);

    if (result.isEmpty()) {
      // Optional.empty() means the room was not found
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Room '" + roomId + "' not found."))
          .build();
    }

    if (!result.get()) {
      // Optional.of(false) means the room still has sensors
      Room room = store.getRoom(roomId);
      if (room == null) {
        // Concurrent deletion between removeRoomIfEmpty and getRoom — room is gone
        return Response.noContent().build();
      }
      throw new RoomNotEmptyException(
          "Cannot delete room '" + roomId + "' because it still has "
              + room.getSensorIds().size() + " sensor(s) assigned: "
              + room.getSensorIds());
    }

    return Response.noContent().build(); // 204 No Content
  }

  // PUT /api/v1/rooms/{roomId} - updates room details, keeps existing sensor
  // links
  @PUT
  @Path("/{roomId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateRoom(@PathParam("roomId") String roomId, Room updatedRoom) {
    Room existing = store.getRoom(roomId);
    if (existing == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(ErrorResponse.notFound("Room '" + roomId + "' not found."))
          .build();
    }

    if (updatedRoom.getName() == null || updatedRoom.getName().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ErrorResponse.badRequest("Room name is required."))
          .build();
    }

    // Preserve the ID and sensor links from the existing record
    updatedRoom.setId(roomId);
    updatedRoom.setSensorIds(existing.getSensorIds());
    store.addRoom(updatedRoom);

    return Response.ok(HateoasHelper.roomResponse(updatedRoom, uriInfo.getBaseUri())).build();
  }
}
