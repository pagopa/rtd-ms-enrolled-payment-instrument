package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.properties.IntegrationFlowKafkaProperties;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter.TokenManagerCardEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter.TokenManagerWalletEventSplitter;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.handler.advice.RetryStateGenerator;
import org.springframework.integration.handler.advice.SpelExpressionRetryStateGenerator;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Configuration
@EnableConfigurationProperties
public class SplitterConfiguration {

  private static final String TARGET_OUT_BINDING = "rtdEnrolledPimProducer-out-0";
  private static final Long FIXED_BACKOFF_INTERVAL = 3000L;

  @Bean
  GenericHandler<TokenManagerCardChanged> cardEventPublisher(StreamBridge bridge) {
    return TokenManagerCardEventPublisher.asHandler(TARGET_OUT_BINDING, bridge);
  }

  @Bean
  public IntegrationFlow splitterFlow(
          KafkaMessageDrivenChannelAdapter<String, TokenManagerWalletChanged> input,
          Function<TokenManagerWalletChanged, List<TokenManagerCardChanged>> splitter,
          RequestHandlerRetryAdvice retryAdvice,
          GenericHandler<TokenManagerCardChanged> cardEventPublisher
  ) {
    return IntegrationFlows.from(input)
            .log(LoggingHandler.Level.INFO, m -> "Received message to split: " + m.getPayload())
            .split(TokenManagerWalletChanged.class, splitter)
            .log(LoggingHandler.Level.INFO, m -> "Split message " + m.getPayload())
            .handle(TokenManagerCardChanged.class, cardEventPublisher, e -> e.advice(retryAdvice))
            .get();
  }

  @Bean
  KafkaMessageDrivenChannelAdapter<String, TokenManagerWalletChanged> input(
          DefaultErrorHandler errorHandler,
          IntegrationFlowKafkaProperties flowKafkaProperties
  ) {
    final var consumerFactory = new DefaultKafkaConsumerFactory<String, TokenManagerWalletChanged>(new HashMap<>(flowKafkaProperties.tkmBulkConsumer));
    final var containerProperties = new ContainerProperties(flowKafkaProperties.tkmBulkConsumer.get("topic"));
    containerProperties.setAckMode(ContainerProperties.AckMode.RECORD);
    final var container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    container.setCommonErrorHandler(errorHandler);
    return Kafka.messageDrivenChannelAdapter(container, KafkaMessageDrivenChannelAdapter.ListenerMode.record).get();
  }

  @Bean
  Function<TokenManagerWalletChanged, List<TokenManagerCardChanged>> splitter() {
    return new TokenManagerWalletEventSplitter();
  }

  /**
   * Retry configuration for splitter integration flow. It defines an infinite retry policy
   * with 3 seconds as backoff policy. Also allow to retry the message processing only when a
   * retryable exceptions happens.
   * Official doc: <a href="https://docs.spring.io/spring-integration/reference/html/messaging-endpoints.html#retry-advice">...</a>
   *
   * @param stateGenerator The state generator to enable statefull retry.
   */
  @Bean
  RequestHandlerRetryAdvice retryAdvice(
          RetryStateGenerator stateGenerator,
          RetryTemplate retryTemplate
  ) {
    final var retryAdvice = new RequestHandlerRetryAdvice();
    retryAdvice.setRetryStateGenerator(stateGenerator);
    retryAdvice.setRetryTemplate(retryTemplate);
    return retryAdvice;
  }

  @Bean
  RetryTemplate retryTemplate(Set<Class<? extends Throwable>> retryableExceptions) {
    return RetryTemplate.builder()
            .infiniteRetry()
            .traversingCauses()
            .retryOn(new ArrayList<>(retryableExceptions))
            .fixedBackoff(FIXED_BACKOFF_INTERVAL)
            .build();
  }

  @Bean
  RetryStateGenerator splitFlowRetryStateGenerator(
          Set<Class<? extends Throwable>> retryableExceptions
  ) {
    final var stateGenerator = new SpelExpressionRetryStateGenerator("headers['correlationId']");
    stateGenerator.setClassifier(new BinaryExceptionClassifier(retryableExceptions, true));
    return stateGenerator;
  }
}
