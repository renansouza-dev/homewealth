package com.renansouza.transactions.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

class RequestLoggingFilterTest {

		private RequestLoggingFilter filter;
		private Logger classLogger;
		private ListAppender<ILoggingEvent> listAppender;

		@BeforeEach
		void setUpLogger() {
				filter = new RequestLoggingFilter();
				classLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
				listAppender = new ListAppender<>();
				listAppender.start();
				classLogger.addAppender(listAppender);
		}

		@AfterEach
		void tearDownLogger() {
				classLogger.detachAppender(listAppender);
		}

		private static Stream<Arguments> provideDataForXFFTest() {
				return Stream.of(
						Arguments.of(",", null),
						Arguments.of("203.0.113.5, 198.51.100.7", "203.0.113.5"),
						Arguments.of(", 203.0.113.5, 198.51.100.7", "203.0.113.5")
				);
		}

		@ParameterizedTest
		@MethodSource("provideDataForXFFTest")
		void shouldExtractIpFromXForwardedFor_andSetSpanAttribute_andCallChain(String xffValue, String ip)
				throws Exception {
				HttpServletRequest req = mock(HttpServletRequest.class);
				when(req.getMethod()).thenReturn("GET");
				when(req.getRequestURI()).thenReturn("/api/test");
				when(req.getHeader("X-Forwarded-For")).thenReturn(xffValue);

				ServletResponse resp = mock(ServletResponse.class);
				FilterChain chain = mock(FilterChain.class);

				Span mockSpan = mock(Span.class);
				try (MockedStatic<Span> spanStatic = Mockito.mockStatic(Span.class)) {
						spanStatic.when(Span::current).thenReturn(mockSpan);

						filter.doFilter(req, resp, chain);

						verify(mockSpan).setAttribute("remote_addr", ip);
						verify(chain, times(1)).doFilter(req, resp);
				}
		}

		@Test
		void multipleParams_ordering_and_inclusionExclusion() throws Exception {
				HttpServletRequest req = mock(HttpServletRequest.class);
				when(req.getHeader("X-Forwarded-For")).thenReturn(null);
				when(req.getRemoteAddr()).thenReturn("127.0.0.1");
				when(req.getMethod()).thenReturn("GET");
				when(req.getRequestURI()).thenReturn("/multi");
				when(req.getQueryString()).thenReturn(
						"page=2&portfolioId=xxx&fromDate=2023-01-01&assetId=99999&size=50&badparam&sortBy=name=");

				ServletResponse resp = mock(ServletResponse.class);
				FilterChain chain = mock(FilterChain.class);

				Span mockSpan = mock(Span.class);
				try (MockedStatic<Span> spanStatic = Mockito.mockStatic(Span.class)) {
						spanStatic.when(Span::current).thenReturn(mockSpan);

						filter.doFilter(req, resp, chain);

						verify(mockSpan).setAttribute("remote_addr", "127.0.0.1");
						verify(chain, times(1)).doFilter(req, resp);

						String message = listAppender.list.stream()
								.filter(e -> e.getLevel() == Level.INFO)
								.map(ILoggingEvent::getFormattedMessage)
								.findFirst()
								.orElse("");

						assertTrue(message.contains("with query params of"), "wrapper for query params is logged");
						assertTrue(message.contains("page=2"), "non-sensitive param is logged");
						assertTrue(message.contains("fromDate=2023-01-01"), "non-sensitive param is logged");
						assertTrue(message.contains("size=50"), "non-sensitive param is logged");
						assertTrue(message.contains("portfolioId=****"), "sensitive param is masked");
						assertTrue(message.contains("assetId=****9999"), "sensitive param is masked");

						assertFalse(message.contains("portfolioId=xxx"), "this value should be masked");
						assertFalse(message.contains("assetId=99999"), "this value should be masked");

						assertFalse(message.contains("badparam"), "malformed entries shouldn't crash and are ignored if no '='");
						assertFalse(message.contains("sortBy=name="),
								"malformed entries shouldn't crash and are ignored if no value after '='");
				}
		}

		@ParameterizedTest
		@NullAndEmptySource
		void shouldUseRemoteAddr_whenNoXff_andLogQuery(String xff) throws Exception {
				HttpServletRequest req = mock(HttpServletRequest.class);
				when(req.getHeader("X-Forwarded-For")).thenReturn(xff);
				when(req.getRemoteAddr()).thenReturn("10.0.0.1");
				when(req.getMethod()).thenReturn("GET");
				when(req.getRequestURI()).thenReturn("/submit");
				when(req.getQueryString()).thenReturn("a=1&b=2");

				ServletResponse resp = mock(ServletResponse.class);
				FilterChain chain = mock(FilterChain.class);

				Span mockSpan = mock(Span.class);
				try (MockedStatic<Span> spanStatic = Mockito.mockStatic(Span.class)) {
						spanStatic.when(Span::current).thenReturn(mockSpan);

						filter.doFilter(req, resp, chain);

						verify(mockSpan).setAttribute("remote_addr", "10.0.0.1");
						verify(chain, times(1)).doFilter(req, resp);
				}
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"",
				"=value",
				"key=",
				"=value&page=1",
				"key=&fromDate=2023-01-01"
		})
		void blankKeyOrValue_isSkipped(String queryString) throws Exception {
				HttpServletRequest req = mock(HttpServletRequest.class);
				when(req.getHeader("X-Forwarded-For")).thenReturn(null);
				when(req.getRemoteAddr()).thenReturn("10.0.0.1");
				when(req.getMethod()).thenReturn("GET");
				when(req.getRequestURI()).thenReturn("/test");
				when(req.getQueryString()).thenReturn(queryString);

				ServletResponse resp = mock(ServletResponse.class);
				FilterChain chain = mock(FilterChain.class);

				Span mockSpan = mock(Span.class);
				try (MockedStatic<Span> spanStatic = Mockito.mockStatic(Span.class)) {
						spanStatic.when(Span::current).thenReturn(mockSpan);

						filter.doFilter(req, resp, chain);

						verify(chain, times(1)).doFilter(req, resp);

						String message = listAppender.list.stream()
								.filter(e -> e.getLevel() == Level.INFO)
								.map(ILoggingEvent::getFormattedMessage)
								.findFirst()
								.orElse("");

						assertFalse(message.contains("=value"), "Blank key entry must be skipped");
						assertFalse(message.contains("key="), "Blank value entry must be skipped");

						if (queryString.contains("page=1")) {
								assertTrue(message.contains("page=1"), "Valid param page must be present");
						}

						if (queryString.contains("fromDate=2023-01-01")) {
								assertTrue(message.contains("fromDate=2023-01-01"), "Valid param fromDate must be present");
						}
				}
		}

}