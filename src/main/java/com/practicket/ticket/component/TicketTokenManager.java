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

    private static final long MIN_TTL_SECONDS = 30;

    public TicketToken issue(String clientKey) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt
                .plus(1, ChronoUnit.MINUTES)
                .truncatedTo(ChronoUnit.MINUTES)
                .minusSeconds(10);

        // 잔여 TTL이 너무 짧으면(30초 미만) 1분 앞으로 이동
        if (Duration.between(issuedAt, expiresAt).getSeconds() < MIN_TTL_SECONDS) {
            expiresAt = expiresAt.plus(1, ChronoUnit.MINUTES);
        }

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
                .setAllowedClockSkewSeconds(5)
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
