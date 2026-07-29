package com.hardwareai.support.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Restores the verified user and tenant context from a bearer token.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwt;

    JwtAuthenticationFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) try {
            var c = jwt.parse(h.substring(7));
            var a = new UsernamePasswordAuthenticationToken(
                    c.getSubject(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + c.get("role", String.class)))
            );
            a.setDetails(c);
            SecurityContextHolder.getContext().setAuthentication(a);
        } catch (Exception e) {
            // Invalid/expired token: leave context unauthenticated and let Security reject the request.
            log.debug("Rejected bearer token on {} {}", request.getMethod(), request.getRequestURI());
        }
        chain.doFilter(request, response);
    }
}
