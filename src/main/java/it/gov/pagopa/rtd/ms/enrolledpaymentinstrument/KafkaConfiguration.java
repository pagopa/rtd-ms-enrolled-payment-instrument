package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.ValidatedConsumer;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.TokenManagerEventAdapter;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes.PaymentInstrumentEventRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class KafkaConfiguration {

  @Bean
  MessageRoutingCallback kafkaRouter(@Autowired ObjectMapper objectMapper) {
    return new PaymentInstrumentEventRouter(
            "tkmUpdateEventConsumer",
            "enrolledPaymentInstrumentConsumer",
            objectMapper
    );
  }

  @Bean
  Consumer<TokenManagerEvent> tkmUpdateEventConsumer(Validator validator, TokenManagerEventAdapter eventAdapter) {
    return new ValidatedConsumer<>(validator, eventAdapter);
  }

  @Bean
  Consumer<TokenManagerWalletEvent> tkmWalletEventConsumer() {
    return message -> log.info("TKM event: {}", message);
  }

  @Bean("retryableExceptions")
  Set<Class<? extends Exception>> kafkaRetryableExceptions() {
    return Set.of(
            SocketTimeoutException.class,
            ConnectException.class,
            UnknownHostException.class,
            IOException.class,
            MongoException.class,
            RecoverableDataAccessException.class,
            TransientDataAccessException.class,
            DuplicateKeyException.class,
            OptimisticLockingFailureException.class
    );
  }

  @Bean("fatalExceptions")
  Set<Class<? extends Exception>> kafkaFatalExceptions() {
    return Set.of(
            IllegalArgumentException.class,
            ConstraintViolationException.class
    );
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
            new FixedBackOff(3000L, FixedBackOff.UNLIMITED_ATTEMPTS)
    );
    //errorHandler.setAckAfterHandle(false);
    //errorHandler.setCommitRecovered(false);
    retryableExceptions.forEach(errorHandler::addRetryableExceptions);
    fatalExceptions.forEach(errorHandler::addNotRetryableExceptions);
    return errorHandler;
  }

}
