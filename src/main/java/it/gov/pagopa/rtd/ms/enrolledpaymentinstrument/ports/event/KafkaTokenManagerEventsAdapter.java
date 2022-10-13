package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import javax.validation.Validator;
import java.util.function.Consumer;

@Slf4j
@Configuration
public class KafkaTokenManagerEventsAdapter {

    @Bean
    Consumer<Message<String>> tkmTokenUpdateConsumer() {
        return message -> log.info("TKM event: {}", message);
    }

    @Bean
    Consumer<TokenManagerEvent> tkmEventConsumer(
            Validator validator
    ) {
        return message -> System.out.println("Dummy tkm update consumer: " + message);
    }
}
