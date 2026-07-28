package com.hardwareai.support.identity;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Restores the verified user and tenant context from a bearer token. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
    } catch (Exception ignored) {}
    chain.doFilter(request, response);
  }
}
