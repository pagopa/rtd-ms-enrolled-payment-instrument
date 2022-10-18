package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations;

import com.mongodb.MongoException;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.KafkaRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.UnknownFormatConversionException;

@Configuration
@EnableMongoRepositories(basePackages = "it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories")
public class AppConfiguration {

  private static final String PRODUCER_BINDING = "rtdRevokedPi-out-0";

  @Bean
  public EnrolledPaymentInstrumentRepository enrolledPaymentInstrumentRepository(
          EnrolledPaymentInstrumentDao dao
  ) {
    return new EnrolledPaymentInstrumentRepositoryImpl(dao);
  }

  @Bean
  public InstrumentRevokeNotificationService revokeService(StreamBridge bridge) {
    return new KafkaRevokeNotificationService(PRODUCER_BINDING, bridge);
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
            OptimisticLockingFailureException.class
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