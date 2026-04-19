package com.w2152988.smartcampus.resource;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Discovery endpoint at GET /api/v1.
 * Returns API metadata and links to the main resources (HATEOAS style)
 * so a client can find all the available endpoints from this single
 * entry point without hardcoding URLs.
 *
 * I'm building the links with UriBuilder and @Path class references
 * so they stay correct even if I rename the resource paths later.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

  // TEMPORARY: demo-only endpoint to prove GenericExceptionMapper catches
  // unhandled Throwables and returns clean JSON with no stack trace.
  // Remove after recording the video demo.
  @GET
  @Path("/test-error")
  public Response testError() {
    throw new RuntimeException("Deliberate test exception — safety net demonstration");
  }

  @GET
  public Response getApiInfo(@Context UriInfo uriInfo) {

    // ── Core API metadata ──────────────────────────────────────
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("name", "Smart Campus Sensor & Room Management API");
    info.put("version", "1.0.0");
    info.put("description",
        "University of Westminster Smart Campus initiative — "
            + "a RESTful API for managing campus rooms, IoT sensors (temperature, "
            + "CO2, occupancy, lighting), and historical sensor readings.");
    info.put("timestamp", Instant.now().toString());

    // ── Administrative contact ─────────────────────────────────
    Map<String, String> contact = new LinkedHashMap<>();
    contact.put("organisation", "University of Westminster — Campus Facilities Management");
    contact.put("administrator", "Smart Campus DevOps Team");
    contact.put("email", "facilities@westminster.ac.uk");
    info.put("contact", contact);

    // HATEOAS links — built with UriBuilder so they match the @Path values
    Map<String, Object> links = new LinkedHashMap<>();

    URI selfUri = uriInfo.getBaseUriBuilder().build();
    URI roomsUri = uriInfo.getBaseUriBuilder().path(RoomResource.class).build();
    URI sensorsUri = uriInfo.getBaseUriBuilder().path(SensorResource.class).build();

    Map<String, String> selfLink = new LinkedHashMap<>();
    selfLink.put("href", selfUri.toString());
    selfLink.put("method", "GET");
    selfLink.put("description", "This discovery endpoint");
    links.put("self", selfLink);

    Map<String, String> roomsLink = new LinkedHashMap<>();
    roomsLink.put("href", roomsUri.toString());
    roomsLink.put("method", "GET");
    roomsLink.put("description", "List or manage all campus rooms");
    links.put("rooms", roomsLink);

    Map<String, String> sensorsLink = new LinkedHashMap<>();
    sensorsLink.put("href", sensorsUri.toString());
    sensorsLink.put("method", "GET");
    sensorsLink.put("description", "List or manage all deployed sensors");
    links.put("sensors", sensorsLink);

    Map<String, String> sensorsByTypeLink = new LinkedHashMap<>();
    sensorsByTypeLink.put("href", sensorsUri.toString() + "?type={sensorType}");
    sensorsByTypeLink.put("method", "GET");
    sensorsByTypeLink.put("description", "Filter sensors by type (e.g. Temperature, CO2, Occupancy)");
    links.put("sensors_by_type", sensorsByTypeLink);

    info.put("_links", links);

    return Response.ok(info).build();
  }
}
