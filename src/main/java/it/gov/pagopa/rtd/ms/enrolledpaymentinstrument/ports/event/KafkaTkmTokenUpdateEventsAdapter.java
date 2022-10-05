package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class KafkaTkmTokenUpdateEventsAdapter {

    @Bean
    Consumer<Message<String>> tkmTokenUpdateConsumer() {
        return message -> log.info("TKM event: {}", message);
    }
}
