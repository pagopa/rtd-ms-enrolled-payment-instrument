package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationEnrollEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
public class ApplicationEnrollEventAdapter implements Consumer<ApplicationEnrollEvent> {

    private final EnrolledPaymentInstrumentService paymentInstrumentService;

    public ApplicationEnrollEventAdapter(EnrolledPaymentInstrumentService paymentInstrumentService) {
        this.paymentInstrumentService = paymentInstrumentService;
    }

    @Override
    public void accept(ApplicationEnrollEvent event) {
        paymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
                event.getHashPan(),
                event.getApp(),
                Operation.valueOf(event.getOperation().toUpperCase()),
                event.getIssuer(),
                event.getNetwork()
        ));
        log.info("Message successfully handled");
    }
}
