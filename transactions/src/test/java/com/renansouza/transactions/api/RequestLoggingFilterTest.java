package com.renansouza.transactions.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class RequestLoggingFilterTest {

		@Test
		void shouldUseFirstIpFromXForwardedFor_andSetSpanAttribute_andCallChain() throws Exception {
				HttpServletRequest req = mock(HttpServletRequest.class);
				when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 198.51.100.7");
				when(req.getMethod()).thenReturn("GET");
				when(req.getRequestURI()).thenReturn("/api/test");

				ServletResponse resp = mock(ServletResponse.class);
				FilterChain chain = mock(FilterChain.class);

				Span mockSpan = mock(Span.class);
				try (MockedStatic<Span> spanStatic = Mockito.mockStatic(Span.class)) {
						spanStatic.when(Span::current).thenReturn(mockSpan);

						RequestLoggingFilter filter = new RequestLoggingFilter();
						filter.doFilter(req, resp, chain);

						verify(mockSpan).setAttribute("remote_addr", "203.0.113.5");
						verify(chain, times(1)).doFilter(req, resp);
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

						RequestLoggingFilter filter = new RequestLoggingFilter();
						filter.doFilter(req, resp, chain);

						verify(mockSpan).setAttribute("remote_addr", "10.0.0.1");
						verify(chain, times(1)).doFilter(req, resp);
				}
		}
}