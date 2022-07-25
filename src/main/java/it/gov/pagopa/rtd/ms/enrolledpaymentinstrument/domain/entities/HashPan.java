package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HashPan {

  private static Pattern regex = Pattern.compile("[a-zA-Z0-9]{64}");

  private final String hashPan;

  public static HashPan create(String hashPan) {
    if (regex.matcher(hashPan).matches()) {
      return new HashPan(hashPan);
    } else {
      throw new IllegalArgumentException("Hash length must be 64 alphanumeric char");
    }
  }

}
