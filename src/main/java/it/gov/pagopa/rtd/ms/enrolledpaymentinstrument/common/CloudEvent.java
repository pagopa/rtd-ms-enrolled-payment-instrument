package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common;

import lombok.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

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

  private String correlationId;

  @Valid
  private T data;

  public static class CloudEventBuilder<T> {
    private String type;
    private String correlationId;
    private T data;

    public CloudEventBuilder<T> withType(String type) {
      this.type = type;
      return this;
    }

    public CloudEventBuilder<T> withCorrelationId(String correlationId) {
      if (Objects.nonNull(correlationId)) {
        this.correlationId = correlationId;
      }
      return this;
    }

    public CloudEventBuilder<T> withData(T data) {
      this.data = data;
      return this;
    }

    public CloudEvent<T> build() {
      return new CloudEvent<>(type, correlationId, data);
    }
  }
}
