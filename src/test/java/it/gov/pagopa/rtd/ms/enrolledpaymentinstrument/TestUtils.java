package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.HashTokenChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;

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
