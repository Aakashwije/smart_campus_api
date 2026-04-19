package com.w2152988.smartcampus.model;

/**
 * A single reading captured by a sensor.
 * Has a UUID, the metric value, and an epoch-ms timestamp.
 */
public class SensorReading {

  private String id; // UUID for this reading
  private long timestamp; // epoch millis
  private double value; // the actual measurement

  // ── Constructors ──────────────────────────────────────────────

  public SensorReading() {
    // Jackson needs this for deserialisation
  }

  public SensorReading(String id, long timestamp, double value) {
    this.id = id;
    this.timestamp = timestamp;
    this.value = value;
  }

  // ── Getters & Setters ─────────────────────────────────────────

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }
}
