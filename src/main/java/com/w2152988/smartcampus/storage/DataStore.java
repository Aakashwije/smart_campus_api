package com.w2152988.smartcampus.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.w2152988.smartcampus.model.Room;
import com.w2152988.smartcampus.model.Sensor;
import com.w2152988.smartcampus.model.SensorReading;

/**
 * Singleton in-memory store for all rooms, sensors and readings.
 * I used ConcurrentHashMap because JAX-RS creates a new resource instance
 * per request, so multiple requests can run at the same time and the map
 * needs to handle that safely without losing data.
 */
public final class DataStore {

  // ── Singleton ─────────────────────────────────────────────────
  private static final DataStore INSTANCE = new DataStore();

  public static DataStore getInstance() {
    return INSTANCE;
  }

  private DataStore() {
    // Add some sample data so the API isn't empty on startup
    seedData();
  }

  // ── Storage Maps ──────────────────────────────────────────────

  /** Room ID → Room */
  private final Map<String, Room> rooms = new ConcurrentHashMap<>();

  /** Sensor ID → Sensor */
  private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

  /** Sensor ID → list of readings */
  private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

  // ── Room Accessors ────────────────────────────────────────────

  public Map<String, Room> getRooms() {
    return rooms;
  }

  public Room getRoom(String id) {
    return rooms.get(id);
  }

  public void addRoom(Room room) {
    rooms.put(room.getId(), room);
  }

  /**
   * Atomic insert — returns null if it worked, or the existing room
   * if the ID was already taken.
   */
  public Room addRoomIfAbsent(Room room) {
    return rooms.putIfAbsent(room.getId(), room);
  }

  // Removes a room only if it has no sensors. Uses compute() to make the
  // check-and-remove atomic.
  // Returns Optional.empty() if the room does not exist.
  // Returns Optional.of(true) if the room was removed.
  // Returns Optional.of(false) if the room still has sensors.
  public Optional<Boolean> removeRoomIfEmpty(String id) {
    final Boolean[] result = { null }; // null = not found, TRUE = removed, FALSE = has sensors
    rooms.compute(id, (key, room) -> {
      if (room == null) {
        result[0] = null; // not found — leave absent
        return null;
      }
      if (room.getSensorIds() == null || room.getSensorIds().isEmpty()) {
        result[0] = Boolean.TRUE;
        return null; // returning null removes the entry
      }
      result[0] = Boolean.FALSE;
      return room; // keep the entry
    });
    return Optional.ofNullable(result[0]);
  }

  public Room removeRoom(String id) {
    return rooms.remove(id);
  }

  // ── Sensor Accessors ──────────────────────────────────────────

  public Map<String, Sensor> getSensors() {
    return sensors;
  }

  public Sensor getSensor(String id) {
    return sensors.get(id);
  }

  public void addSensor(Sensor sensor) {
    sensors.put(sensor.getId(), sensor);
    // Make sure there's a readings list ready for this sensor
    readings.putIfAbsent(sensor.getId(), Collections.synchronizedList(new ArrayList<>()));
  }

  // Atomic insert — returns null if it worked, or the existing sensor if the ID
  // was taken
  public Sensor addSensorIfAbsent(Sensor sensor) {
    Sensor existing = sensors.putIfAbsent(sensor.getId(), sensor);
    if (existing == null) {
      readings.putIfAbsent(sensor.getId(), Collections.synchronizedList(new ArrayList<>()));
    }
    return existing;
  }

  public Sensor removeSensor(String id) {
    readings.remove(id);
    return sensors.remove(id);
  }

  // ── Reading Accessors ─────────────────────────────────────────

  public List<SensorReading> getReadings(String sensorId) {
    return readings.getOrDefault(sensorId, Collections.synchronizedList(new ArrayList<>()));
  }

  public void addReading(String sensorId, SensorReading reading) {
    readings.computeIfAbsent(sensorId, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(reading);
  }

  // ── Seed Data ─────────────────────────────────────────────────

  private void seedData() {
    // Sample rooms
    Room lib301 = new Room("LIB-301", "Library Quiet Study", 40);
    Room eng102 = new Room("ENG-102", "Engineering Lab A", 60);
    Room room101 = new Room("ROOM-101", "Lecture Hall A (Renovated)", 75);
    rooms.put(lib301.getId(), lib301);
    rooms.put(eng102.getId(), eng102);
    rooms.put(room101.getId(), room101);

    // Sample sensors linked to the rooms above
    Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
    Sensor co2001 = new Sensor("CO2-001", "CO2", "ACTIVE", 415.0, "LIB-301");
    Sensor occ001 = new Sensor("OCC-001", "Occupancy", "ACTIVE", 12.0, "ENG-102");

    sensors.put(temp001.getId(), temp001);
    sensors.put(co2001.getId(), co2001);
    sensors.put(occ001.getId(), occ001);

    lib301.addSensorId("TEMP-001");
    lib301.addSensorId("CO2-001");
    eng102.addSensorId("OCC-001");

    // Empty reading lists for each sensor
    readings.put(temp001.getId(), Collections.synchronizedList(new ArrayList<>()));
    readings.put(co2001.getId(), Collections.synchronizedList(new ArrayList<>()));
    readings.put(occ001.getId(), Collections.synchronizedList(new ArrayList<>()));
  }
}
