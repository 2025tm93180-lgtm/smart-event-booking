package com.team.ticketnotification.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ticketnotification.common.ApiErrors;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean JwtDecoder jwtDecoder(@Value("${app.security.jwt-secret}") String secret,
        @Value("${app.security.jwt-issuer}") String issuer,@Value("${app.security.jwt-audience}") String audience) {
        byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);
        if(bytes.length<32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes.");
        var decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes,"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> claims=jwt -> {
            boolean valid=jwt.getAudience().contains(audience) && jwt.getSubject()!=null && !jwt.getSubject().isBlank() && jwt.getExpiresAt()!=null;
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Required token claims missing or invalid",null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),claims));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http,ObjectMapper mapper) throws Exception {
        var converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> "ADMIN".equals(jwt.getClaimAsString("role")) ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) : List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        http.csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/health","/health/**").permitAll().anyRequest().authenticated())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setHeader("WWW-Authenticate","Bearer");res.setContentType("application/json");mapper.writeValue(res.getOutputStream(),ApiErrors.body(401,"UNAUTHORIZED","A valid bearer token is required.",req.getRequestURI()));}))
            .exceptionHandling(e->e.accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");mapper.writeValue(res.getOutputStream(),ApiErrors.body(403,"FORBIDDEN","Access denied.",req.getRequestURI()));}));
        return http.build();
    }
}
