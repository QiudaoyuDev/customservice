package com.hardwareai.support.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication boundary for the management console.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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
            .filter(UserAccount::enabled);
        if (u.isEmpty() || !encoder.matches(request.password(), u.get().passwordHash())) {
            log.warn("Login failed (userFound={})", u.isPresent());
            throw new IllegalArgumentException("Invalid email or password");
        }
        log.info("Login succeeded role={}", u.get().role().name());
        return new Token(jwt.issue(u.get()), u.get().email(), u.get().role().name());
    }

    record Login(@Email String email, @Size(min = 8, max = 128) String password) {
    }

    record Token(String accessToken, String email, String role) {
    }
}
