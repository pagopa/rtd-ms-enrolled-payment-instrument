package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardAction;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.HashTokenAction;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerEvent;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TestUtils {

  private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final Random random = new Random();

  public static HashPan generateRandomHashPan() {
    return HashPan.create(randomString(64));
  }

  public static String randomString(int length) {
    return IntStream.range(0, length)
            .mapToObj(i -> "" + ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())))
            .collect(Collectors.joining(""));
  }

  public static TokenManagerEvent.TokenManagerEventBuilder prepareRandomTokenManagerEvent(CardAction action) {
    return TokenManagerEvent.builder()
            .par(randomString(4))
            .hashPan(generateRandomHashPan().getValue())
            .taxCode(randomString(10))
            .hashTokens(generateRandomHashTokenEvent(10))
            .operation(action);
  }

  public static List<TokenManagerEvent.HashTokenEvent> generateRandomHashTokenEvent(int which) {
    final var random = new Random();
    return IntStream.range(0, which)
            .mapToObj(i -> new TokenManagerEvent.HashTokenEvent(
                    generateRandomHashPan().getValue(),
                    random.nextDouble() < 0.5 ? HashTokenAction.DELETE : HashTokenAction.UPDATE
            ))
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
}
