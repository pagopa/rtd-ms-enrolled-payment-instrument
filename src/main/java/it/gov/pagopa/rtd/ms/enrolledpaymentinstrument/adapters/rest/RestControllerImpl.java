package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.SomethingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@Slf4j
public class RestControllerImpl implements
    it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest.RestController {

  private final SomethingService somethingService;

  @Autowired
  RestControllerImpl(
      SomethingService somethingService
  ) {
    this.somethingService = somethingService;
  }

  @Override
  public String postSomething(String something) {
    log.info("Doing something");
    final var result = this.somethingService.processSomething("1", something);
    log.info("Result {}", result);
    return "{}";
  }
}
