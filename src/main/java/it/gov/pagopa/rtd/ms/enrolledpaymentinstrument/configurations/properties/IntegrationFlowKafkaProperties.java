package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "integration-flow")
@Data
public final class IntegrationFlowKafkaProperties {
  public final Map<String, String> tkmBulkConsumer;
}
