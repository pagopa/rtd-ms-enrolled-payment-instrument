package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.resource;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
@Builder
public class EnrolledPaymentInstrumentDto {
    @NotNull
    @NotBlank
    private String hashPan;

    @NotNull
    @NotBlank
    private Set<String> app;

    private String issuer;

    private String network;
}
