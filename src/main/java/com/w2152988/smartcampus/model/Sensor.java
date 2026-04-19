package com.w2152988.smartcampus.model;

/**
 * Represents an IoT sensor on the Smart Campus.
 * Each sensor belongs to exactly one Room (linked by roomId).
 */
public class Sensor {

  private String id; // e.g. "TEMP-001"
  private String type; // e.g. "Temperature", "CO2", "Occupancy"
  private String status; // "ACTIVE", "MAINTENANCE", or "OFFLINE"
  private double currentValue; // latest reading value
  private String roomId; // which room this sensor is in

  // ── Constructors ──────────────────────────────────────────────

  public Sensor() {
    // Jackson needs this for deserialisation
  }

  public Sensor(String id, String type, String status, double currentValue, String roomId) {
    this.id = id;
    this.type = type;
    this.status = status;
    this.currentValue = currentValue;
    this.roomId = roomId;
  }

  // ── Getters & Setters ─────────────────────────────────────────

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public double getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(double currentValue) {
    this.currentValue = currentValue;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }
}
