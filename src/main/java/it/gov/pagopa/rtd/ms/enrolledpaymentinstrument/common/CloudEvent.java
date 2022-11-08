package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CloudEvent<T> {

  public static <T> CloudEventBuilder<T> builder() {
    return new CloudEventBuilder<>();
  }

  @NonNull
  @NotBlank
  private String type;

  @Valid
  private T data;

  public static class CloudEventBuilder<T> {
    private String type;
    private T data;

    public CloudEventBuilder<T> withType(String type) {
      this.type = type;
      return this;
    }

    public CloudEventBuilder<T> withData(T data) {
      this.data = data;
      return this;
    }

    public CloudEvent<T> build() {
      return new CloudEvent<>(type, data);
    }
  }
}
