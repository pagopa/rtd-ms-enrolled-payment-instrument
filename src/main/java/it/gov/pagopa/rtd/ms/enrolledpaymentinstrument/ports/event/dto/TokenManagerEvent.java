package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenManagerEvent {

  @NotNull
  @NotBlank
  private String hashPan;

  private String taxCode;

  private String par;

  private List<HashTokenEvent> hashTokens;

  @NotNull
  @NotBlank
  private CardAction operation;

  @Data
  public static class HashTokenEvent {
    final String hashToken;
    final HashTokenAction operation;
  }

  public static List<TkmUpdateCommand.TkmTokenCommand> buildTokenUpdateCommands(TokenManagerEvent event) {
    return event.getHashTokens()
            .stream()
            .map(it -> new TkmUpdateCommand.TkmTokenCommand(
                            it.getHashToken(),
                            it.getOperation() == HashTokenAction.UPDATE ?
                                    TkmUpdateCommand.TkmTokenCommand.Action.UPDATE : TkmUpdateCommand.TkmTokenCommand.Action.DELETE
                    )
            )
            .collect(Collectors.toList());
  }
}
