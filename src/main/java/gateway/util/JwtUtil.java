package gateway.util;

import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Getter
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private final KeyProvider keyProvider;

    public JwtUtil(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(keyProvider.getPublicKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return false;
    }

}
