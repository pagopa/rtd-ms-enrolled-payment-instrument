package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.SpringVersion;

@SpringBootApplication
public class RtdMsEnrolledPaymentInstrumentApplication {

  public static void main(String[] args) {
    System.out.println(SpringVersion.getVersion());
    SpringApplication.run(RtdMsEnrolledPaymentInstrumentApplication.class, args);
  }
}