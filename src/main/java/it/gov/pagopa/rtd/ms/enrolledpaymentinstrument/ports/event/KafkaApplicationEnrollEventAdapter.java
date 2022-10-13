package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationEnrollEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.Validator;
import java.util.function.Consumer;

@Slf4j
@Configuration
class KafkaApplicationEnrollEventAdapter {

    private final EnrolledPaymentInstrumentService paymentInstrumentService;

    public KafkaApplicationEnrollEventAdapter(EnrolledPaymentInstrumentService paymentInstrumentService) {
        this.paymentInstrumentService = paymentInstrumentService;
    }

    @SneakyThrows
    @Bean
    Consumer<ApplicationEnrollEvent> enrolledPaymentInstrumentConsumer(
            Validator validator
    ) {
        return new ValidatedConsumer<>(validator, message -> {
            paymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
                    message.getHashPan(),
                    message.getApp(),
                    Operation.valueOf(message.getOperation().toUpperCase()),
                    message.getIssuer(),
                    message.getNetwork()
            ));
            log.info("Message successfully handled");
        });
    }
}
