package com.renansouza.transactions;

import com.renansouza.transactions.infra.InstallOpenTelemetryAppender;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class InstallOpenTelemetryAppenderTest {

  @Test
  void shouldInstallAppenderWhenAfterPropertiesSetIsCalled() {
    OpenTelemetry openTelemetry = mock(OpenTelemetry.class);

    try (MockedStatic<OpenTelemetryAppender> mockedStatic = mockStatic(OpenTelemetryAppender.class)) {

      InstallOpenTelemetryAppender installer =
          new InstallOpenTelemetryAppender(openTelemetry);

      installer.afterPropertiesSet();

      mockedStatic.verify(() -> OpenTelemetryAppender.install(openTelemetry));
    }
  }
}