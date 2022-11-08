package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;

class ChainRevokeNotificationServiceTest {

  private InstrumentRevokeNotificationService notificationService1;
  private InstrumentRevokeNotificationService notificationService2;
  private ChainRevokeNotificationService chainRevokeNotificationService;

  @BeforeEach
  void setup() {
    notificationService1 = Mockito.mock(InstrumentRevokeNotificationService.class);
    notificationService2 = Mockito.mock(InstrumentRevokeNotificationService.class);
    chainRevokeNotificationService = new ChainRevokeNotificationService(List.of(
            notificationService1,
            notificationService2
    ));
  }

  @AfterEach
  void teardown() {
    Mockito.reset(notificationService1, notificationService2);
  }

  @Test
  void whenANotificationServiceFailsThenChainIsInterrupted() {
    Mockito.doReturn(false).when(notificationService1).notifyRevoke(any(), any(), any());
    Mockito.doReturn(true).when(notificationService2).notifyRevoke(any(), any(), any());

    chainRevokeNotificationService.notifyRevoke(Set.of(), "taxCode", TestUtils.generateRandomHashPan());

    Mockito.verify(notificationService1, Mockito.times(1)).notifyRevoke(any(), any(), any());
    Mockito.verify(notificationService2, Mockito.times(0)).notifyRevoke(any(), any(), any());
  }

  @Test
  void whenAllNotificationServiceCompleteSuccessfullyThenReturnsTrue() {
    Mockito.doReturn(true).when(notificationService1).notifyRevoke(any(), any(), any());
    Mockito.doReturn(true).when(notificationService2).notifyRevoke(any(), any(), any());

    assertThat(chainRevokeNotificationService.notifyRevoke(Set.of(), "taxCode", TestUtils.generateRandomHashPan())).isTrue();
  }

  @Test
  void whenOneNotificationServiceFailThenReturnsFalse() {
    Mockito.doReturn(true).when(notificationService1).notifyRevoke(any(), any(), any());
    Mockito.doReturn(false).when(notificationService2).notifyRevoke(any(), any(), any());

    assertThat(chainRevokeNotificationService.notifyRevoke(Set.of(), "taxCode", TestUtils.generateRandomHashPan())).isFalse();
  }

  @Test
  void whenThrowUnhandledExceptionsThenThrowIt() {
    Mockito.doThrow(new RuntimeException("")).when(notificationService1).notifyRevoke(any(), any(), any());
    Mockito.doReturn(true).when(notificationService2).notifyRevoke(any(), any(), any());

    assertThrowsExactly(RuntimeException.class, () -> chainRevokeNotificationService.notifyRevoke(any(), any(), any()));
    Mockito.verify(notificationService2, Mockito.times(0)).notifyRevoke(any(), any(), any());
  }
}