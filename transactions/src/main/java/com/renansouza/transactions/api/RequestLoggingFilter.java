package com.renansouza.transactions.api;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 Servlet filter that logs incoming HTTP requests and records the client IP on the current OpenTelemetry span.
 Responsibilities:
 Extract the client IP from the request preferencing the "X-Forwarded-For" header when present (first entry).
 Set the extracted IP as a span attribute named "remote_addr" on the current OpenTelemetry Span.
 Write an INFO log entry with a human-readable description of the request (method, URI and optional query string).
 Delegate request processing to the filter chain.
 Notes:
 This class casts ServletRequest to HttpServletRequest; it expects HTTP requests.
 The span manipulation uses Span.current(); if your environment provides automatic instrumentation, no extra span
 creation is required.</li>
 Header parsing treats blank/empty X-Forwarded-For as absent and falls back to {@code HttpServletRequest#getRemoteAddr()}.
 @see jakarta.servlet.Filter
 */
@Component
public class RequestLoggingFilter implements Filter {

		private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

		/**
		 Logs request info, sets the extracted client IP on the current OpenTelemetry Span under the
		 attribute key {@code "remote_addr"}, and continues the filter chain.

		 @param request the incoming {@link ServletRequest} (must be an {@link HttpServletRequest})
		 @param response the outgoing {@link ServletResponse}
		 @param chain the filter chain to delegate to
		 @throws IOException if an I/O error occurs during filtering
		 @throws ServletException if the downstream filter or servlet throws a {@link ServletException} */
		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				HttpServletRequest req = (HttpServletRequest) request;

				String ip = extractClientIp(req);
				Span.current().setAttribute("remote_addr", ip);

				StringBuilder sb = new StringBuilder();
				sb.append("Incoming Request from ").append(ip)
						.append(" to ").append(req.getMethod())
						.append(" ").append(req.getRequestURI());
				if (Objects.nonNull(req.getQueryString())) sb.append(" with query params of ").append(req.getQueryString());

				log.atInfo().setMessage(sb.toString()).log();
				chain.doFilter(request, response);
		}

		/**
		 Extracts the client IP address from the given HTTP request.
		 Extraction strategy:
		 If the {@code X-Forwarded-For} header is present and non-blank, return the first comma-separated entry (trimmed).
		 Otherwise, return {@code HttpServletRequest#getRemoteAddr()}.

		 @param req the HTTP servlet request
		 @return the extracted client IP string (never {@code null} if {@code req} provides a non-null remote address) */
		private String extractClientIp(HttpServletRequest req) {
				String xff = req.getHeader("X-Forwarded-For");
				if (xff != null && !xff.isBlank()) {
						return xff.split(",")[0].trim();
				}

				return req.getRemoteAddr();
		}

}