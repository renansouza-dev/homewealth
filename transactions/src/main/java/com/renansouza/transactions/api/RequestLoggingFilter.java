package com.renansouza.transactions.api;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Servlet filter that logs incoming HTTP requests and records the client IP on the current OpenTelemetry span.
 * Responsibilities: Extract the client IP from the request preferencing the "X-Forwarded-For" header when present
 * (first entry). Set the extracted IP as a span attribute named "remote_addr" on the current OpenTelemetry Span. Write
 * an INFO log entry with a human-readable description of the request (method, URI and optional query string). Delegate
 * request processing to the filter chain. Notes: This class casts ServletRequest to HttpServletRequest; it expects HTTP
 * requests. The span manipulation uses Span.current(); if your environment provides automatic instrumentation, no extra
 * span creation is required.</li> Header parsing treats blank/empty X-Forwarded-For as absent and falls back to
 * {@code HttpServletRequest#getRemoteAddr()}.
 *
 * @see jakarta.servlet.Filter
 */
@Component
public class RequestLoggingFilter implements Filter {

		private static final String MASK = "****";
		private static final Set<String> SENSITIVE_KEYS = Set.of("portfolioId", "assetId");
		private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

		/**
		 * Logs request info, sets the extracted client IP on the current OpenTelemetry Span under the attribute key
		 * {@code "remote_addr"}, and continues the filter chain.
		 *
		 * @param request  the incoming {@link ServletRequest} (must be an {@link HttpServletRequest})
		 * @param response the outgoing {@link ServletResponse}
		 * @param chain    the filter chain to delegate to
		 * @throws IOException      if an I/O error occurs during filtering
		 * @throws ServletException if the downstream filter or servlet throws a {@link ServletException}
		 */
		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
				HttpServletRequest req = (HttpServletRequest) request;

				Span.current().setAttribute("remote_addr", extractClientIp(req));

				log.atInfo()
						.setMessage("Incoming Request %s %s %s".formatted(
								req.getMethod(),
								req.getRequestURI(),
								parseAndPrintQueryParams(req.getQueryString())))
						.log();

				chain.doFilter(request, response);
		}

		/**
		 * Parses a URL query string and return valid key-value pairs to the given {@link String}.
		 *
		 * <p>Pairs are separated by {@code &}. A pair is considered valid only when it contains exactly
		 * one {@code =} delimiter with a non-blank key and a non-blank value. Invalid entries — such as missing delimiters,
		 * blank keys, or blank values — are silently skipped.
		 *
		 * <p>Values whose keys are listed in {@link #SENSITIVE_KEYS} are masked via {@link #maskValue}
		 * before being appended.
		 *
		 * @param queryString the raw query string from the request (e.g. {@code "page=0&size=20"}), expected to be non-null
		 *                    and non-blank at call site
		 * @return the {@link String} to append the formatted pairs to; pairs are comma-separated (e.g.
		 * {@code "page=0, size=20"})
		 */
		private String parseAndPrintQueryParams(String queryString) {
				if (queryString == null || queryString.isEmpty()) {
						return "";
				}

				StringBuilder sb = new StringBuilder();
				String[] pairs = queryString.split("&");
				boolean first = true;

				for (String pair : pairs) {
						String[] parts = pair.split("=", -1);
						if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
								continue;
						}

						String key = parts[0].trim();
						String value = SENSITIVE_KEYS.contains(key) ? maskValue(parts[1].trim()) : parts[1].trim();

						if (!first) {
								sb.append(", ");
						}

						sb.append(key).append("=").append(value);
						first = false;
				}

				return sb.isEmpty() ? Strings.EMPTY : " with query params of [ " + sb + " ]";
		}

		/**
		 * Masks a sensitive query parameter value, preserving only its last 4 characters as a hint.
		 *
		 * <p>If the value is 4 characters or shorter it is fully replaced by {@link #MASK}, giving
		 * no hint of the original content. Otherwise, {@link #MASK} is prepended to the last 4 characters (e.g.
		 * {@code "abc12345"} → {@code "****2345"}).
		 *
		 * @param value the plain-text value to mask; expected to be non-null and non-blank at call site
		 * @return the masked representation of the value
		 */
		private String maskValue(String value) {
				int keep = 4;
				if (value.length() <= keep) {
						return MASK;
				}
				String tail = value.substring(value.length() - keep);
				return MASK + tail;
		}

		/**
		 * Extracts the client IP address from the given HTTP request. Extraction strategy: If the {@code X-Forwarded-For}
		 * header is present and non-blank, return the first comma-separated entry (trimmed). Otherwise, return
		 * {@code HttpServletRequest#getRemoteAddr()}.
		 *
		 * @param req the HTTP servlet request
		 * @return the extracted client IP string (never {@code null} if {@code req} provides a non-null remote address)
		 */
		private String extractClientIp(HttpServletRequest req) {
				String xff = req.getHeader("X-Forwarded-For");
				if (xff != null && !xff.isBlank()) {
						for (String part : xff.split(",")) {
								String candidate = part.trim();
								if (!candidate.isBlank()) {
										return candidate;
								}
						}
				}

				return req.getRemoteAddr();
		}

}