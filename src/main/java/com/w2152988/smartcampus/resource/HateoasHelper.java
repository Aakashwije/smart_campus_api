package com.w2152988.smartcampus.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny helper that builds HATEOAS link maps so we don't repeat the same
 * boilerplate in every resource method. Each link is a map with "href",
 * "method", and "description" keys — consistent with the discovery endpoint.
 */
final class HateoasHelper {

  private HateoasHelper() {
    // utility class
  }

  /**
   * Creates a single link entry: { "href": ..., "method": ..., "description": ...
   * }
   */
  static Map<String, String> link(String href, String method, String description) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("href", href);
    map.put("method", method);
    map.put("description", description);
    return map;
  }

  /**
   * Wraps a domain object and a set of HATEOAS links into a single response
   * map with the entity fields plus an "_links" key. We copy bean properties
   * manually to keep it simple and avoid pulling in a reflection library.
   */
  static Map<String, Object> wrap(Map<String, Object> fields, Map<String, Object> links) {
    Map<String, Object> envelope = new LinkedHashMap<>(fields);
    envelope.put("_links", links);
    return envelope;
  }

  /** Builds a Room response map with HATEOAS links. */
  static Map<String, Object> roomResponse(com.w2152988.smartcampus.model.Room room, URI baseUri) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("id", room.getId());
    fields.put("name", room.getName());
    fields.put("capacity", room.getCapacity());
    fields.put("sensorIds", room.getSensorIds());

    String roomHref = baseUri.toString() + "rooms/" + room.getId();

    Map<String, Object> links = new LinkedHashMap<>();
    links.put("self", link(roomHref, "GET", "This room"));
    links.put("update", link(roomHref, "PUT", "Update this room"));
    links.put("delete", link(roomHref, "DELETE", "Delete this room (must have no sensors)"));
    links.put("collection", link(baseUri.toString() + "rooms", "GET", "All rooms"));

    return wrap(fields, links);
  }

  /** Builds a Sensor response map with HATEOAS links. */
  static Map<String, Object> sensorResponse(com.w2152988.smartcampus.model.Sensor sensor, URI baseUri) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("id", sensor.getId());
    fields.put("type", sensor.getType());
    fields.put("status", sensor.getStatus());
    fields.put("currentValue", sensor.getCurrentValue());
    fields.put("roomId", sensor.getRoomId());

    String sensorHref = baseUri.toString() + "sensors/" + sensor.getId();
    String roomHref = baseUri.toString() + "rooms/" + sensor.getRoomId();

    Map<String, Object> links = new LinkedHashMap<>();
    links.put("self", link(sensorHref, "GET", "This sensor"));
    links.put("update", link(sensorHref, "PUT", "Update this sensor"));
    links.put("delete", link(sensorHref, "DELETE", "Delete this sensor"));
    links.put("readings", link(sensorHref + "/readings", "GET", "Reading history for this sensor"));
    links.put("room", link(roomHref, "GET", "The room this sensor belongs to"));
    links.put("collection", link(baseUri.toString() + "sensors", "GET", "All sensors"));

    return wrap(fields, links);
  }

  /** Builds a SensorReading response map with HATEOAS links. */
  static Map<String, Object> readingResponse(
      com.w2152988.smartcampus.model.SensorReading reading, String sensorId, URI baseUri) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("id", reading.getId());
    fields.put("timestamp", reading.getTimestamp());
    fields.put("value", reading.getValue());

    String sensorHref = baseUri.toString() + "sensors/" + sensorId;
    String readingHref = sensorHref + "/readings/" + reading.getId();

    Map<String, Object> links = new LinkedHashMap<>();
    links.put("self", link(readingHref, "GET", "This reading"));
    links.put("sensor", link(sensorHref, "GET", "The sensor that recorded this reading"));
    links.put("collection", link(sensorHref + "/readings", "GET", "All readings for this sensor"));

    return wrap(fields, links);
  }
}
