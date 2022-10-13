package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
public class TkmUpdateCommand {

  @NotNull
  @NotBlank
  private final String hashPan;

  private final String par;

  private final List<TkmTokenCommand> tokens;

  @Data
  @AllArgsConstructor
  public static class TkmTokenCommand {

    @NotNull
    @NotBlank
    final String hashPan;

    final Action action;

    public enum Action {
      UPDATE,
      DELETE
    }
  }
}