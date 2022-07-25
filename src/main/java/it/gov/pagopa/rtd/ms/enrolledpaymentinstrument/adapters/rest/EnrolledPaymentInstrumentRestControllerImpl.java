package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@Slf4j
public class EnrolledPaymentInstrumentRestControllerImpl implements
    EnrolledPaymentInstrumentRestController {

  private final EnrolledPaymentInstrumentService enrolledPaymentInstrumentService;

  @Autowired
  EnrolledPaymentInstrumentRestControllerImpl(
      EnrolledPaymentInstrumentService enrolledPaymentInstrumentService
  ) {
    this.enrolledPaymentInstrumentService = enrolledPaymentInstrumentService;
  }

  @Override
  public List<String> getActiveHashPan() {
    enrolledPaymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
        "2ac01c6a86e0ada464d7682888d3e01009ed57ff0bfea09575d842039e1030ef",
        "id_pay",
        true
    ));
    return Collections.emptyList();
  }
}
