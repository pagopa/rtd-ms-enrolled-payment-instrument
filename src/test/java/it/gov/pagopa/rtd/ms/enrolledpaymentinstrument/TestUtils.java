package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;

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
