package com.renansouza.transactions.infra;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@Tag("unit")
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