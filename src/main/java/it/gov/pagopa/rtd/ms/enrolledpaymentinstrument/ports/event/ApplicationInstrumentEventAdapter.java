package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.CorrelationIdService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentAdded;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentDeleted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
public class ApplicationInstrumentEventAdapter {

    private final EnrolledPaymentInstrumentService paymentInstrumentService;

    public ApplicationInstrumentEventAdapter(EnrolledPaymentInstrumentService paymentInstrumentService) {
        this.paymentInstrumentService = paymentInstrumentService;
    }

    public Consumer<CloudEvent<ApplicationInstrumentAdded>> addedEventConsumer() {
        return event -> {
            final var data = event.getData();
            final var command = new EnrollPaymentInstrumentCommand(
                    data.getHashPan(),
                    data.getApplication(),
                    Operation.CREATE,
                    "",
                    ""
            );
            paymentInstrumentService.handle(command);
            log.info("Message successfully handled");
        };
    }

    public Consumer<CloudEvent<ApplicationInstrumentDeleted>> deleteEventConsumer() {
        return event -> {
            final var data = event.getData();
            final var command = new EnrollPaymentInstrumentCommand(
                    data.getHashPan(),
                    data.getApplication(),
                    Operation.DELETE,
                    "",
                    ""
            );
            paymentInstrumentService.handle(command);
            log.info("Message successfully handled");
        };
    }
}
