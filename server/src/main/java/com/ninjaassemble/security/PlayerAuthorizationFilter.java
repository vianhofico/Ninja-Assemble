package com.ninjaassemble.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class PlayerAuthorizationFilter extends OncePerRequestFilter {
    private static final Pattern PLAY = Pattern.compile("^/api/v1/play/([0-9a-fA-F-]{36})(?:/.*)?$");
    private static final Pattern PLAYER = Pattern.compile("^/api/v1/players/([0-9a-fA-F-]{36})(?:/.*)?$");
    private final SessionTokenService tokens;

    public PlayerAuthorizationFilter(SessionTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID playerId = playerId(request.getRequestURI());
        if (playerId == null) {
            chain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()).trim() : "";
        if (!tokens.verify(token, playerId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED_PLAYER_SESSION\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static UUID playerId(String uri) {
        for (Pattern pattern : new Pattern[]{PLAY, PLAYER}) {
            Matcher matcher = pattern.matcher(uri == null ? "" : uri);
            if (!matcher.matches()) continue;
            try { return UUID.fromString(matcher.group(1)); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
}
