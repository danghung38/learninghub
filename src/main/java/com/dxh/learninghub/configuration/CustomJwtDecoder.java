package com.dxh.learninghub.configuration;



import com.dxh.learninghub.dto.request.IntrospectRequest;
import com.dxh.learninghub.service.impl.AuthenticationServiceImpl;
import com.nimbusds.jose.JOSEException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Autowired
    private AuthenticationServiceImpl authenticationServiceImpl;

    private NimbusJwtDecoder nimbusJwtDecoder;

    @PostConstruct
    public void init() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
        this.nimbusJwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            var response = authenticationServiceImpl.introspect(
                    IntrospectRequest.builder().token(token).build());

            if (!response.valid())
                throw new JwtException("Token invalid");

        } catch (JOSEException | ParseException e) {
            throw new JwtException(e.getMessage());
        }

        return nimbusJwtDecoder.decode(token);
    }
}

