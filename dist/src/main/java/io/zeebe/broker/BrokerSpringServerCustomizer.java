package io.zeebe.broker;

import io.zeebe.broker.system.configuration.BrokerCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class BrokerSpringServerCustomizer
    implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

  @Autowired BrokerCfg brokerCfg;

  @Override
  public void customize(ConfigurableServletWebServerFactory server) {
    //    final var networkCfg = brokerCfg.getNetwork();
    //    // trigger application of defaults so that the monitoring config no longer has null values
    //    networkCfg.applyDefaults();
    //
    //    final var monitoringApiCfg = networkCfg.getMonitoringApi();
    //
    //    try {
    //      server.setAddress(getByName(monitoringApiCfg.getHost()));
    //    } catch (UnknownHostException e) {
    //      throw new UncheckedExecutionException(e.getLocalizedMessage(), e);
    //    }
    //    // server.setPort(monitoringApiCfg.getPort());
  }
}
