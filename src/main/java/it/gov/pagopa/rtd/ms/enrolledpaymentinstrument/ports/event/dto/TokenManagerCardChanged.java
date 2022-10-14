package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenManagerCardChanged {

  @NotNull
  @NotBlank
  private String hashPan;

  private String taxCode;

  private String par;

  private List<HashTokenEvent> hashTokens;

  @NotNull
  private CardChangeType changeType;

  @Data
  public static class HashTokenEvent {
    final String hashToken;
    final HashTokenChangeType changeType;
  }

  public List<TkmUpdateCommand.TkmTokenCommand> toTkmTokenCommand() {
    return Optional.ofNullable(getHashTokens())
            .orElse(Collections.emptyList())
            .stream()
            .map(it -> new TkmUpdateCommand.TkmTokenCommand(
                            it.getHashToken(),
                            it.getChangeType() == HashTokenChangeType.UPDATE ?
                                    TkmUpdateCommand.TkmTokenCommand.Action.UPDATE :
                                    TkmUpdateCommand.TkmTokenCommand.Action.DELETE
                    )
            )
            .collect(Collectors.toList());
  }
}
