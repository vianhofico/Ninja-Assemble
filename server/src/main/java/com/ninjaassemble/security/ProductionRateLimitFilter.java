package com.ninjaassemble.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public final class ProductionRateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final int readLimit;
    private final int writeLimit;
    private final int guestLimit;

    public ProductionRateLimitFilter(StringRedisTemplate redis,
                                     @Value("${game.release.rate-limit.read-per-minute:300}") int readLimit,
                                     @Value("${game.release.rate-limit.write-per-minute:120}") int writeLimit,
                                     @Value("${game.release.rate-limit.guest-per-minute:20}") int guestLimit) {
        if (readLimit <= 0 || writeLimit <= 0 || guestLimit <= 0) throw new IllegalArgumentException("rate limits must be positive");
        this.redis = redis; this.readLimit = readLimit; this.writeLimit = writeLimit; this.guestLimit = guestLimit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        if (!path.startsWith("/api/")) { chain.doFilter(request, response); return; }
        int limit = path.equals("/api/v1/players/guest") ? guestLimit : "GET".equalsIgnoreCase(request.getMethod()) ? readLimit : writeLimit;
        String client = clientKey(request);
        long window = System.currentTimeMillis() / 60_000L;
        String key = "release-rate:" + window + ":" + bucket(path) + ":" + client;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) redis.expire(key, Duration.ofSeconds(75));
            if (count != null && count > limit) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"RATE_LIMITED\"}");
                return;
            }
        } catch (RuntimeException unavailable) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"RATE_LIMIT_BACKEND_UNAVAILABLE\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String stable = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()).trim()
                : request.getRemoteAddr();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((stable == null ? "unknown" : stable).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception impossible) {
            return Integer.toUnsignedString((stable == null ? "unknown" : stable).hashCode(), 36);
        }
    }

    private static String bucket(String path) {
        if (path.equals("/api/v1/players/guest")) return "guest";
        return path.startsWith("/api/v1/play/") ? "play" : "api";
    }
}
