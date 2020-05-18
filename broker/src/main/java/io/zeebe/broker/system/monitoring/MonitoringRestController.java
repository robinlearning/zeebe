package io.zeebe.broker.system.monitoring;

import static io.prometheus.client.exporter.common.TextFormat.CONTENT_TYPE_004;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.zeebe.broker.Loggers;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitoringRestController {

  private static final String BROKER_READY_STATUS_URI = "/ready";
  private static final String METRICS_URI = "/metrics";
  private static final String BROKER_HEALTH_STATUS_URI = "/health";

  @Autowired private CollectorRegistry metricsRegistry;

  //  @GetMapping(METRICS_URI)
  //  public ModelAndView metrics() {
  //    return new ModelAndView("forward:/actuator/prometheus");
  //  }

  @GetMapping(value = BROKER_HEALTH_STATUS_URI)
  public ResponseEntity<String> health() {
    final boolean brokerHealthy = brokerHealthCheckService.isBrokerHealthy();

    final HttpStatus status;
    if (brokerHealthy) {
      status = HttpStatus.NO_CONTENT;
    } else {
      status = HttpStatus.SERVICE_UNAVAILABLE;
    }
    return new ResponseEntity<>(status);
  }

  @GetMapping(value = METRICS_URI, produces = CONTENT_TYPE_004)
  public ResponseEntity<String> metrics(
      @RequestParam(name = "name[]", required = false) String[] names) {
    final ByteBuf buf = Unpooled.buffer();

    try (final Writer writer = new StringWriter()) {
      TextFormat.write004(
          writer, metricsRegistry.filteredMetricFamilySamples(metricsFilter(names)));
      return new ResponseEntity<>(writer.toString(), HttpStatus.OK);
    } catch (final IOException e) {
      Loggers.SYSTEM_LOGGER.warn("Failed to respond to metrics request", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = BROKER_READY_STATUS_URI)
  public ResponseEntity<String> ready() {
    final boolean brokerReady = brokerHealthCheckService.isBrokerReady();

    final HttpStatus status;
    if (brokerReady) {
      status = HttpStatus.NO_CONTENT;
    } else {
      status = HttpStatus.SERVICE_UNAVAILABLE;
    }
    return new ResponseEntity<>(status);
  }

  private static Set<String> metricsFilter(final String[] names) {
    if (names != null && !(names.length == 0)) {
      return new HashSet<>(Arrays.asList(names));
    } else {
      return Collections.emptySet();
    }
  }
}
