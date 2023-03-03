package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.ExportCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentAdded;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.PaymentInstrumentExported;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

@Slf4j
@Component
public class ExportEventAdapter {

  private final EnrolledPaymentInstrumentService enrolledPaymentInstrumentService;

  public ExportEventAdapter(EnrolledPaymentInstrumentService enrolledPaymentInstrumentService) {
    this.enrolledPaymentInstrumentService = enrolledPaymentInstrumentService;
  }

  public Consumer<CloudEvent<PaymentInstrumentExported>> paymentInstrumentExportedConsumer() {
    return event -> {
      final var data = event.getData();
      final var command = new ExportCommand(data.getPaymentInstrumentId(), OffsetDateTime.now());
      enrolledPaymentInstrumentService.handle(command);
      log.info("Export event handler ends successfully");
    };
  }
}
