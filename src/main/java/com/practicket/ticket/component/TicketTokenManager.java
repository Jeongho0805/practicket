package com.practicket.ticket.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.TicketException;
import com.practicket.ticket.domain.TicketToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class TicketTokenManager {

    private final SecretKey secretKey;
    private static final String TOKEN_TYPE = "TICKET_TOKEN";
    private static final String TYPE_CLAIM = "type";

    public TicketTokenManager(@Value("${app.secret.ticket-token}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public TicketToken issue(String clientKey) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(1, ChronoUnit.DAYS);

        long ttlSec = Duration.between(issuedAt, expiresAt).getSeconds();
        String jti = UUID.randomUUID().toString();
        String jwt = Jwts.builder()
                .setSubject(clientKey)
                .setId(jti)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .claim(TYPE_CLAIM, TOKEN_TYPE)
                .signWith(secretKey)
                .compact();
        return new TicketToken(jwt, jti, expiresAt, Duration.ofSeconds(ttlSec));
    }

    public Claims parseAndValidate(String jwt) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
        String type = claims.get(TYPE_CLAIM, String.class);
        if (!TOKEN_TYPE.equals(type)) {
            throw new TicketException(ErrorCode.TICKET_TOKEN_IS_NOT_VALID);
        }
        return claims;
    }
}
