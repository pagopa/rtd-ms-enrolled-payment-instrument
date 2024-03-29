package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
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

  public static final String TYPE = "TokenManagerCardChanged";

  @NotNull
  @NotBlank
  private String hashPan;

  private String taxCode;

  private String par;

  private List<HashTokenEvent> hashTokens;

  private OffsetDateTime timestamp;

  @NotNull
  private CardChangeType changeType;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HashTokenEvent {
    private String hashToken;
    private HashTokenChangeType changeType;
  }

  public List<TkmUpdateCommand.TkmTokenCommand> toTkmTokenCommand() {
    return Optional.ofNullable(getHashTokens())
            .orElse(Collections.emptyList())
            .stream()
            .map(it -> new TkmUpdateCommand.TkmTokenCommand(
                            it.getHashToken(),
                            it.getChangeType() == HashTokenChangeType.INSERT_UPDATE ?
                                    TkmUpdateCommand.TkmTokenCommand.Action.UPDATE :
                                    TkmUpdateCommand.TkmTokenCommand.Action.DELETE
                    )
            )
            .collect(Collectors.toList());
  }
}
