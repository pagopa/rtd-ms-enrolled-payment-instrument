package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.HashTokenChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.PaymentInstrumentExported;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import java.util.stream.Stream;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TestUtils {

  private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final Random random = new Random();

  public static HashPan generateRandomHashPan() {
    return HashPan.create(randomString(64));
  }

  public static String generateRandomHashPanAsString() {
    return generateRandomHashPan().getValue();
  }

  public static String randomString(int length) {
    return IntStream.range(0, length)
            .mapToObj(i -> "" + ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())))
            .collect(Collectors.joining(""));
  }

  public static TokenManagerCardChanged.TokenManagerCardChangedBuilder prepareRandomTokenManagerEvent(CardChangeType action) {
    return TokenManagerCardChanged.builder()
            .par(randomString(4))
            .hashPan(generateRandomHashPan().getValue())
            .taxCode(randomString(10))
            .hashTokens(generateRandomHashTokenEvent(10))
            .changeType(action);
  }

  public static List<TokenManagerCardChanged.HashTokenEvent> generateRandomHashTokenEvent(int which) {
    final var random = new Random();
    return IntStream.range(0, which)
            .mapToObj(i -> new TokenManagerCardChanged.HashTokenEvent(
                    generateRandomHashPan().getValue(),
                    random.nextDouble() < 0.5 ? HashTokenChangeType.DELETE : HashTokenChangeType.INSERT_UPDATE
            ))
            .collect(Collectors.toList());
  }

  public static List<PaymentInstrumentExported> generateRandomPaymentInstrumentExportedEvent(int which) {
    return IntStream.range(0, which)
            .mapToObj(i -> new PaymentInstrumentExported(generateRandomHashPanAsString()))
            .collect(Collectors.toList());
  }

  public static List<TkmUpdateCommand.TkmTokenCommand> generateRandomUpdateTokenCommand(int which) {
    final var random = new Random();
    return IntStream.range(0, which)
            .mapToObj(i -> new TkmUpdateCommand.TkmTokenCommand(
                            TestUtils.generateRandomHashPan().getValue(),
                            random.nextDouble() < 0.5 ?
                                    TkmUpdateCommand.TkmTokenCommand.Action.DELETE :
                                    TkmUpdateCommand.TkmTokenCommand.Action.UPDATE
                    )
            ).collect(Collectors.toList());
  }

  /**
   * Return a list of fixed hashpan which guarantee kakfa partitions
   * are equally distribution over 3 partitions.
   */
  public static Stream<String> partitionedHashPans() {
    return Stream.of(
        "4tX28JnnyCPmjFSjueDsPHUPwJIEo3Sc0rGSNPQz7fZINB08Fc6S0Q1LxChSCkZL",
        "97VpsIPFFlEqQtf7etwoz4agNUUM5eMzcBc3uo7mA2kxooGloFeHQdEQ8bb7D0c3",
        "d7aOTEgQbEitJ7vC7qSlbUIaRx1UD74BoPWwxHXMnwHXCaxUFEOV7ZPOy9Gikb1j",
        "dCuE8niQFo7mgqMMfEAK7s2mRUndIztOT2isiTqYMoL9h0NPfMO9L3JtUjiKTMt8",
        "DDcLbQwjbd6FwqjT4ZOlb9Ps3W9GKLlF7eQaQOhJB5kmkTjnzv0qaRQNm1kmSGcn"
    );
  }

  public static <R> Function<Message<?>, R> castMessageBody() {
    return message -> {
      return (R) message.getPayload();
    };
  }

  public static <R> Function<Message<String>, R> parseTo(ObjectMapper mapper, Class<R> clazz) {
    return it -> {
      try {
        return mapper.readValue(it.getPayload(), clazz);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <R> Function<Message<String>, R> parseTo(ObjectMapper mapper, TypeReference<R> clazz) {
    return it -> {
      try {
        return mapper.readValue(it.getPayload(), clazz);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
