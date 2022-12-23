package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.FailedToNotifyRevoke;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Validated
public class TkmPaymentInstrumentService {

  private final EnrolledPaymentInstrumentRepository repository;
  private final InstrumentRevokeNotificationService revokeService;
  private final DomainEventPublisher domainEventPublisher;

  public TkmPaymentInstrumentService(
          EnrolledPaymentInstrumentRepository repository,
          InstrumentRevokeNotificationService revokeService,
          DomainEventPublisher domainEventPublisher
  ) {
    this.repository = repository;
    this.revokeService = revokeService;
    this.domainEventPublisher = domainEventPublisher;
  }

  public void handle(@Valid TkmUpdateCommand command) {
    final var paymentInstrumentOrEmpty = repository.findByHashPan(command.getHashPan());
    if (paymentInstrumentOrEmpty.isPresent()) {
      final var paymentInstrument = paymentInstrumentOrEmpty.get();
      final var updateAndRemove = Optional.ofNullable(command.getTokens())
              .orElse(Collections.emptyList())
              .stream()
              .collect(Collectors.groupingByConcurrent(
                      TkmUpdateCommand.TkmTokenCommand::getAction,
                      Collectors.mapping(value -> HashPan.create(value.getHashPan()), Collectors.toSet())
              ));

      final var toUpdate = updateAndRemove.getOrDefault(TkmUpdateCommand.TkmTokenCommand.Action.UPDATE, Collections.emptySet());
      final var toDelete = updateAndRemove.getOrDefault(TkmUpdateCommand.TkmTokenCommand.Action.DELETE, Collections.emptySet());

      log.info("Token to update {}, to delete {}", toUpdate.size(), toDelete.size());

      paymentInstrument.associatePar(command.getPar());

      toUpdate.forEach(paymentInstrument::addHashPanChild);
      toDelete.forEach(paymentInstrument::removeHashPanChild);

      domainEventPublisher.handle(paymentInstrument);
      repository.save(paymentInstrument);
    } else {
      log.info("Received tkm update event for non existing instrument");
    }
  }

  public void handle(@Valid TkmRevokeCommand command) {
    final var hashPan = HashPan.create(command.getHashPan());
    final var paymentInstrumentOrEmpty = repository.findByHashPan(command.getHashPan());
    final var applications = paymentInstrumentOrEmpty.map(EnrolledPaymentInstrument::getEnabledApps).orElse(Set.of());
    if (paymentInstrumentOrEmpty.isPresent()) {
      final var paymentInstrument = paymentInstrumentOrEmpty.get();
      paymentInstrument.revokeInstrument();
      domainEventPublisher.handle(paymentInstrument);
      repository.save(paymentInstrument);
    } else {
      log.warn("Handled revoke command on non existing card");
    }
    if (revokeService.notifyRevoke(applications, command.getTaxCode(), hashPan)) {
      log.info("Revoke notification sent");
    } else {
      throw new FailedToNotifyRevoke();
    }
  }
}
