package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;


import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.SomethingService;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {

  @Bean
  public SomethingService somethingService() {
    return new SomethingService();
  }

}
