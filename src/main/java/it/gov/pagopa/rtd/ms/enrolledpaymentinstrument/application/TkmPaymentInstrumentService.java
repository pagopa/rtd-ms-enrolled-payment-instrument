package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.VirtualEnrollError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.ChildTokenAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.ParAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Validated
public class TkmPaymentInstrumentService {

  private final EnrolledPaymentInstrumentRepository repository;
  private final InstrumentRevokeNotificationService revokeService;
  private final VirtualEnrollService virtualEnrollService;

  public TkmPaymentInstrumentService(
          EnrolledPaymentInstrumentRepository repository,
          InstrumentRevokeNotificationService revokeService,
          VirtualEnrollService virtualEnrollService
  ) {
    this.repository = repository;
    this.revokeService = revokeService;
    this.virtualEnrollService = virtualEnrollService;
  }

  public void handle(@Valid TkmUpdateCommand command) {
    final var hashPan = HashPan.create(command.getHashPan());
    final var paymentInstrument = repository.findByHashPan(command.getHashPan())
            .orElse(EnrolledPaymentInstrument.createUnEnrolledInstrument(hashPan, "", ""));

    final var updateAndRemove = Optional.ofNullable(command.getTokens())
            .orElse(Collections.emptyList())
            .stream()
            .collect(Collectors.partitioningBy(it -> it.getAction() == TkmUpdateCommand.TkmTokenCommand.Action.UPDATE));

    log.info("Token to update {}, to delete {}", updateAndRemove.get(true).size(), updateAndRemove.get(false).size());

    paymentInstrument.associatePar(command.getPar());

    paymentInstrument.addHashPanChildren(
            updateAndRemove.get(true)
                    .stream()
                    .map(it -> HashPan.create(it.getHashPan()))
                    .collect(Collectors.toSet())
    );

    paymentInstrument.removeHashPanChildren(
            updateAndRemove.get(false)
                    .stream()
                    .map(it -> HashPan.create(it.getHashPan()))
                    .collect(Collectors.toSet())
    );

    repository.save(paymentInstrument);

    handleDomainEvents(paymentInstrument);
  }

  public void handle(@Valid TkmRevokeCommand command) {
    final var hashPan = HashPan.create(command.getHashPan());
    final var paymentInstrumentOrEmpty = repository.findByHashPan(command.getHashPan());
    if (paymentInstrumentOrEmpty.isPresent()) {
      final var paymentInstrument = paymentInstrumentOrEmpty.get();
      paymentInstrument.revokeInstrument();
      repository.save(paymentInstrument);
    } else {
      log.warn("Handled revoke command on non existing card");
    }
    if (revokeService.notifyRevoke(command.getTaxCode(), hashPan)) {
      log.info("Revoke notification sent");
    } else {
      log.error("Failed to send revoke notification");
    }
  }

  private void handleParAssociated(ParAssociated parAssociatedEvent) {
    log.info("Handling Par Associated Event, doing virtual enroll");
    performVirtualEnroll(parAssociatedEvent.getHashPan(), parAssociatedEvent.getPar());
  }

  private void handleChildTokenAssociated(ChildTokenAssociated event) {
    log.info("Handling Child Token Associated Event, doing virtual enroll");
    performVirtualEnroll(event.getChildHashPan(), event.getPar());
  }

  private void performVirtualEnroll(HashPan hashPan, String par) {
    if (virtualEnrollService.enroll(hashPan, par)) {
      log.info("Virtual enroll done");
    } else {
      log.error("Failed during virtual enroll");
      throw new VirtualEnrollError();
    }
  }

  private void handleDomainEvents(EnrolledPaymentInstrument paymentInstrument) {
    paymentInstrument.getDomainEvents().forEach(event -> {
      if (event instanceof ParAssociated) handleParAssociated((ParAssociated) event);
      if (event instanceof ChildTokenAssociated) handleChildTokenAssociated((ChildTokenAssociated) event);
    });
    paymentInstrument.clearDomainEvents();
  }
}
