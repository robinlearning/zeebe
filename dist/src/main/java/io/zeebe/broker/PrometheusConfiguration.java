package io.zeebe.broker;

import io.prometheus.client.CollectorRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(PrometheusMetricsExportAutoConfiguration.class)
public class PrometheusConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public CollectorRegistry collectorRegistry() {
    return CollectorRegistry.defaultRegistry;
  }
}
