package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations;

import com.mongodb.MongoException;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.FailedToNotifyRevoke;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.ChainRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.BPDRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack.KafkaEnrollAckService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.revoke.KafkaRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.tkm.TkmTokenFinder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.util.ObjectUtils;

import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UnknownFormatConversionException;

@Configuration
public class AppConfiguration {

  private static final String RTD_TO_APP_BINDING = "rtdToApp-out-0";

  @Value("${revoke-notification.bpd-url:}")
  private String baseUrlBpdDeleteCard;

  @Value("${instrument-token-finder.url:#{null}}")
  private String baseUrlTokenFinder;

  @Value("${instrument-token-finder.api-key:}")
  private String apiKeyTokenFinder;

  @Bean
  public EnrollAckService enrollAckService(StreamBridge bridge) {
    return new KafkaEnrollAckService(bridge, RTD_TO_APP_BINDING);
  }

  @Bean
  public InstrumentRevokeNotificationService revokeService(StreamBridge bridge) {
    return new ChainRevokeNotificationService(List.of(
            ObjectUtils.isEmpty(baseUrlBpdDeleteCard) ?
                    BPDRevokeNotificationService.fake() :
                    BPDRevokeNotificationService.fromUrl(baseUrlBpdDeleteCard),
            new KafkaRevokeNotificationService(RTD_TO_APP_BINDING, bridge)
    ));
  }

  @Bean
  public InstrumentTokenFinder instrumentTokenFinder() {
    return Objects.isNull(baseUrlTokenFinder) ?
            InstrumentTokenFinder.fake(LoggerFactory.getLogger(InstrumentTokenFinder.class)) :
            TkmTokenFinder.fromUrl(baseUrlTokenFinder, apiKeyTokenFinder);
  }

  /**
   * A list of exceptions considered as "transient", so these are used as
   * retryable exceptions with kafka consumers.
   */
  @Bean("retryableExceptions")
  Set<Class<? extends Exception>> consumerRetryableExceptions() {
    return Set.of(
            SocketTimeoutException.class,
            ConnectException.class,
            UnknownHostException.class,
            IOException.class,
            MongoException.class,
            RecoverableDataAccessException.class,
            TransientDataAccessException.class,
            DuplicateKeyException.class,
            OptimisticLockingFailureException.class,
            FailedToNotifyRevoke.class,
            EnrollAckError.class
    );
  }

  /**
   * A list of non-transient exceptions. Tipically validation and schema error
   * where no recovery operation are available.
   */
  @Bean("fatalExceptions")
  Set<Class<? extends Exception>> consumerFatalExceptions() {
    return Set.of(
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            UnknownFormatConversionException.class
    );
  }

}
