package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.ValidatedConsumer;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.ApplicationInstrumentEventAdapter;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.TokenManagerEventAdapter;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentAdded;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentDeleted;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes.PaymentInstrumentEventRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import javax.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class KafkaConfiguration {

  private static final Long FIXED_BACKOFF_INTERVAL = 3000L;
  private static final Map<String, String> routingMap = new HashMap<>();

  static {
    routingMap.put(ApplicationInstrumentAdded.TYPE, "applicationInstrumentAddedConsumer");
    routingMap.put(ApplicationInstrumentDeleted.TYPE, "applicationInstrumentDeletedConsumer");
    routingMap.put(TokenManagerCardChanged.TYPE, "tkmUpdateEventConsumer");
  }

  @Bean
  MessageRoutingCallback kafkaRouter(@Autowired ObjectMapper objectMapper) {
    return new PaymentInstrumentEventRouter(routingMap, objectMapper);
  }

  @Bean
  Consumer<CloudEvent<ApplicationInstrumentAdded>> applicationInstrumentAddedConsumer(
          Validator validator,
          ApplicationInstrumentEventAdapter eventAdapter
  ) {
    return new ValidatedConsumer<>(validator, eventAdapter.addedEventConsumer());
  }

  @Bean
  Consumer<CloudEvent<ApplicationInstrumentDeleted>> applicationInstrumentDeletedConsumer(
          Validator validator,
          ApplicationInstrumentEventAdapter eventAdapter
  ) {
    return new ValidatedConsumer<>(validator, eventAdapter.deleteEventConsumer());
  }

  @Bean
  Consumer<CloudEvent<TokenManagerCardChanged>> tkmUpdateEventConsumer(Validator validator, TokenManagerEventAdapter eventAdapter) {
    return new ValidatedConsumer<>(validator, eventAdapter);
  }

  @Bean
  ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> listenerCustomization(
          DefaultErrorHandler errorHandler
  ) {
    return (container, dest, group) -> {
      container.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
      container.setCommonErrorHandler(errorHandler);
    };
  }

  @Bean
  DefaultErrorHandler errorHandler(
          Set<Class<? extends Exception>> retryableExceptions,
          Set<Class<? extends Exception>> fatalExceptions
  ) {
    // previously called seek to error handler
    // this error handler allow to always retry the retryable exceptions
    // like db connection error or write error. While allow to set
    // not retryable exceptions like validation error which cannot be recovered with a retry.
    final var errorHandler = new DefaultErrorHandler(
            new FixedBackOff(FIXED_BACKOFF_INTERVAL, FixedBackOff.UNLIMITED_ATTEMPTS)
    );
    errorHandler.defaultFalse();
    retryableExceptions.forEach(errorHandler::addRetryableExceptions);
    fatalExceptions.forEach(errorHandler::addNotRetryableExceptions);
    return errorHandler;
  }

}
