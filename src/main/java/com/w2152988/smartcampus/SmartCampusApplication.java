package com.w2152988.smartcampus;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application config class — sets the base path to /api/v1 and
 * registers all resource packages, exception mappers, and filters.
 *
 * Extends ResourceConfig (Jersey) so package scanning works automatically.
 * Tomcat's Servlet container picks this up via the @ApplicationPath annotation
 * and Jersey's ServletContainerInitializer — no web.xml required.
 *
 * JAX-RS creates a new resource instance per request, which is why
 * shared state is kept in a separate DataStore singleton backed by
 * ConcurrentHashMap.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

  public SmartCampusApplication() {
    // Scan for @Path resources, @Provider mappers and filters
    packages("com.w2152988.smartcampus.resource",
        "com.w2152988.smartcampus.exception.mapper",
        "com.w2152988.smartcampus.filter");

    // Register Jackson so our POJOs get serialised to/from JSON
    register(JacksonFeature.class);
  }
}
