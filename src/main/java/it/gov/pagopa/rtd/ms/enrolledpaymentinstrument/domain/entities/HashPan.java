package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import java.util.regex.Pattern;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Domain value object HashPan.
 *
 * It provides a factory method to validate and create a new valid hashpan instance
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HashPan {

  private static Pattern regex = Pattern.compile("[a-zA-Z0-9]{64}");

  private final String value;

  public static HashPan create(@Valid @NotNull @NotBlank String hashPan) {
    if (regex.matcher(hashPan).matches()) {
      return new HashPan(hashPan);
    } else {
      throw new IllegalArgumentException("Hash length must be 64 alphanumeric char");
    }
  }

}
