package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import com.mongodb.MongoException;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Set;

@Configuration
public class KafkaConfiguration {

    @Bean("retryableExceptions")
    Set<Class<? extends Exception>> kafkaRetryableExceptions() {
        return Set.of(
                SocketTimeoutException.class,
                ConnectException.class,
                UnknownHostException.class,
                IOException.class,
                MongoException.class
        );
    }

    @Bean("fatalExceptions")
    Set<Class<? extends Exception>> kafkaFatalExceptions() {
        return Set.of(IllegalArgumentException.class);
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
        errorHandler.setAckAfterHandle(false);
        errorHandler.setCommitRecovered(false);
        retryableExceptions.forEach(errorHandler::addRetryableExceptions);
        fatalExceptions.forEach(errorHandler::addNotRetryableExceptions);
        return errorHandler;
    }


}