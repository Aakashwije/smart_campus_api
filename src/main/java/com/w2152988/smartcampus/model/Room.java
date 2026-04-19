package com.w2152988.smartcampus.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a room on the campus. Holds a list of sensor IDs
 * for the sensors that are deployed in it.
 */
public class Room {

  private String id; // e.g. "LIB-301"
  private String name; // e.g. "Library Quiet Study"
  private int capacity; // max occupancy
  private List<String> sensorIds = new CopyOnWriteArrayList<>(); // sensor IDs in this room

  // ── Constructors ──────────────────────────────────────────────

  public Room() {
    // Jackson needs this for deserialisation
  }

  public Room(String id, String name, int capacity) {
    this.id = id;
    this.name = name;
    this.capacity = capacity;
  }

  // ── Getters & Setters ─────────────────────────────────────────

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public List<String> getSensorIds() {
    return sensorIds;
  }

  public void setSensorIds(List<String> sensorIds) {
    this.sensorIds = new CopyOnWriteArrayList<>(sensorIds);
  }

  // ── Helper Methods ────────────────────────────────────────────

  // Adds a sensor ID if it's not already in the list (thread-safe).
  // We check the runtime type because Jackson may deserialise sensorIds
  // as a plain ArrayList — in that case we fall back to contains + add.
  public void addSensorId(String sensorId) {
    if (this.sensorIds instanceof CopyOnWriteArrayList) {
      ((CopyOnWriteArrayList<String>) this.sensorIds).addIfAbsent(sensorId);
    } else if (!this.sensorIds.contains(sensorId)) {
      this.sensorIds.add(sensorId);
    }
  }

  public void removeSensorId(String sensorId) {
    this.sensorIds.remove(sensorId);
  }
}
