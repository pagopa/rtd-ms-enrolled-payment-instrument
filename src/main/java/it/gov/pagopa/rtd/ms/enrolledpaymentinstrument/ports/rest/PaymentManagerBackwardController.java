package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Validated
public interface PaymentManagerBackwardController {

  @DeleteMapping("/{hashPan}")
  void revokeCard(
          @NotNull @NotBlank @PathVariable String hashPan,
          @NotNull @NotBlank @RequestParam("taxCode") String taxCode
  );
}
