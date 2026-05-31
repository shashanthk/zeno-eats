package com.zenoeats.userservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RsaKeyConfig {

    @Value("${jwt.private-key}")
    private String privateKeyBase64;

    @Value("${jwt.public-key}")
    private String publicKeyBase64;

    @Bean
    public RSAPrivateKey jwtPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    @Bean
    public RSAPublicKey jwtPublicKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(keyBytes));
    }
}
