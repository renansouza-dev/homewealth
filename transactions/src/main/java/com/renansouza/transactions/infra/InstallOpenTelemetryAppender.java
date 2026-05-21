package com.renansouza.transactions.infra;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Spring component that installs the OpenTelemetry log appender after the bean is initialized.
 *
 * <p>Implements {@link InitializingBean} to hook into the Spring lifecycle and invoke
 * {@link OpenTelemetryAppender#install(OpenTelemetry)} once all properties have been set,
 * ensuring that log events are bridged to the OpenTelemetry SDK from application startup.
 *
 * @see OpenTelemetryAppender
 * @see InitializingBean
 */
@Component
public class InstallOpenTelemetryAppender implements InitializingBean {
    
    private final OpenTelemetry openTelemetry;

    /**
     * Creates a new instance with the provided {@link OpenTelemetry} instance.
     *
     * @param openTelemetry the OpenTelemetry SDK instance used to configure the log appender
     */
    public InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Installs the OpenTelemetry log appender after all Spring-managed properties have been set.
     *
     * <p>This method is called automatically by the Spring container during bean initialization,
     * guaranteeing that the {@link OpenTelemetry} instance is fully configured before
     * {@link OpenTelemetryAppender#install(OpenTelemetry)} is invoked.
     *
     * @see OpenTelemetryAppender#install(OpenTelemetry)
     */
    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }

}