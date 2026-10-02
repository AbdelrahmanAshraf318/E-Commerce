package com.example.eCommerce.security;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads RSA keys from PEM files.
 * Private key: PKCS#8 ("BEGIN PRIVATE KEY"). Public key: X.509 / SPKI ("BEGIN PUBLIC KEY").
 */
public final class KeyUtils
{
    private KeyUtils()
    {
    }

    public static RSAPrivateKey loadPrivateKey(Resource pem)
    {
        byte[] der = decodePem(pem, "PRIVATE KEY");
        try
        {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Invalid RSA private key: " + pem.getDescription(), e);
        }
    }

    public static RSAPublicKey loadPublicKey(Resource pem)
    {
        byte[] der = decodePem(pem, "PUBLIC KEY");
        try
        {
            // Public keys are X.509 encoded - PKCS8EncodedKeySpec only works for private keys.
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Invalid RSA public key: " + pem.getDescription(), e);
        }
    }

    private static byte[] decodePem(Resource pem, String type)
    {
        if (!pem.exists())
            throw new IllegalStateException("Key file not found: " + pem.getDescription()
                    + ". See README.md -> 'Local security setup' to generate a key pair.");

        try (InputStream inputStream = pem.getInputStream())
        {
            String base64 = new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII)
                    .replace("-----BEGIN " + type + "-----", "")
                    .replace("-----END " + type + "-----", "")
                    .replaceAll("\\s+", "");
            return Base64.getDecoder().decode(base64);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not read key file: " + pem.getDescription(), e);
        }
    }
}
