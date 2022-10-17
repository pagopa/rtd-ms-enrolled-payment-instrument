package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter.TokenManagerWalletEventSplitter;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.*;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.handler.advice.RetryStateGenerator;
import org.springframework.integration.handler.advice.SpelExpressionRetryStateGenerator;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Configuration
public class SplitterConfiguration {

  private static final String TO_SPLIT_BINDING = "tkmWalletEventConsumer-in-0";
  private static final String TARGET_OUT_BINDING = "rtdEnrolledPimProducer-out-0";
  private static final Long FIXED_BACKOFF_INTERVAL = 3000L;

  @Bean
  Function<TokenManagerWalletChanged, List<TokenManagerCardChanged>> splitter() {
    return new TokenManagerWalletEventSplitter();
  }

  @Bean
  public IntegrationFlow splitterFlow(StreamBridge bridge, RequestHandlerRetryAdvice retryAdvice) {
    return IntegrationFlows.from(TO_SPLIT_BINDING, true)
            .log(LoggingHandler.Level.INFO, m -> "Received message to split: " + m.getHeaders())
            .split(TokenManagerWalletChanged.class, splitter())
            .log(LoggingHandler.Level.INFO, m -> "Split message " + m.getPayload())
            .handle(TokenManagerCardChanged.class,
                    (payload, headers) -> {
                      bridge.send(TARGET_OUT_BINDING, MessageBuilder.withPayload(payload)
                              .setHeader("partitionKey", payload.getHashPan())
                              .build()
                      );
                      return null;
                    },
                    e -> e.advice(retryAdvice)
            ).get();
  }

  /**
   * Retry configuration for splitter integration flow. It defines an infinite retry policy
   * with 3 seconds as backoff policy. Also allow to retry the message processing only when a
   * retryable exceptions happens.
   * Official doc: <a href="https://docs.spring.io/spring-integration/reference/html/messaging-endpoints.html#retry-advice">...</a>
   *
   * @param stateGenerator      The state generator to enable statefull retry.
   * @param retryableExceptions A Set of retryable exceptions
   */
  @Bean
  RequestHandlerRetryAdvice retryAdvice(
          RetryStateGenerator stateGenerator,
          Set<Class<? extends Throwable>> retryableExceptions
  ) {
    final var retryAdvice = new RequestHandlerRetryAdvice();
    final var retryTemplate = RetryTemplate.builder()
            .infiniteRetry()
            .traversingCauses()
            .retryOn(new ArrayList<>(retryableExceptions))
            .fixedBackoff(FIXED_BACKOFF_INTERVAL)
            .build();
    retryAdvice.setRetryStateGenerator(stateGenerator);
    retryAdvice.setRetryTemplate(retryTemplate);
    return retryAdvice;
  }

  @Bean
  RetryStateGenerator splitFlowRetryStateGenerator(
          Set<Class<? extends Throwable>> retryableExceptions
  ) {
    final var stateGenerator = new SpelExpressionRetryStateGenerator(
            "headers['correlationId']"
    );
    stateGenerator.setClassifier(new BinaryExceptionClassifier(retryableExceptions, true));
    return stateGenerator;
  }
}
