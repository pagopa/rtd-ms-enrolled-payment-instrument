package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
public class TokenManagerEventAdapter implements Consumer<TokenManagerCardChanged> {

  private final TkmPaymentInstrumentService tkmPaymentInstrumentService;

  public TokenManagerEventAdapter(TkmPaymentInstrumentService tkmPaymentInstrumentService) {
    this.tkmPaymentInstrumentService = tkmPaymentInstrumentService;
  }

  @Override
  public void accept(TokenManagerCardChanged event) {
    if (event.getChangeType() == CardChangeType.INSERT_UPDATE) {
      final var tokenUpdateCommands = event.toTkmTokenCommand();
      tkmPaymentInstrumentService.handle(new TkmUpdateCommand(event.getHashPan(), event.getPar(), tokenUpdateCommands));
    } else if (event.getChangeType() == CardChangeType.REVOKE) {
      tkmPaymentInstrumentService.handle(new TkmRevokeCommand(event.getTaxCode(), event.getHashPan(), event.getPar()));
    }
  }
}
