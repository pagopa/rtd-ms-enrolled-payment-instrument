package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class SomethingService {

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  public String processSomething(String id, String something) {
    final var enrolledPaymentInstrument = repository.findById(id);
    final var updatedEnrolledPayment = enrolledPaymentInstrument.map((it) -> {
      it.doSomething(something);
      return it;
    });

    return updatedEnrolledPayment.map((it) -> {
      repository.save(it);
      return it.getSomething1();
    }).orElse("");
  }

}
