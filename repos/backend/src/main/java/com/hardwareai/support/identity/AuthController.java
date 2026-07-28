package com.hardwareai.support.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/** Authentication boundary for the management console. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserAccountRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  AuthController(UserAccountRepository users, PasswordEncoder encoder, JwtService jwt) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  @PostMapping("/login")
  public Token login(@Valid @RequestBody Login request) {
    var u = users
      .findByEmail(request.email())
      .filter(UserAccount::enabled)
      .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    if (!encoder.matches(request.password(), u.passwordHash())) throw new IllegalArgumentException(
      "Invalid email or password"
    );
    return new Token(jwt.issue(u), u.email(), u.role().name());
  }

  record Login(@Email String email, @Size(min = 8, max = 128) String password) {}

  record Token(String accessToken, String email, String role) {}
}
