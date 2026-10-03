package com.example.eCommerce.security;

import lombok.experimental.UtilityClass;
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
 * <p>
 * A PEM file is just Base64 text wrapped in BEGIN/END lines. The Base64 decodes to DER - the binary
 * encoding of the key - which Java's {@link KeyFactory} understands once it is told the format
 * (PKCS#8 for private keys, X.509 for public keys).
 */
@UtilityClass
public class KeyUtils
{
    /**
     * Reads the RSA private key used to <b>sign</b> tokens.
     * <p>
     * Steps: PEM text -> strip header/footer/whitespace -> Base64-decode to DER bytes ->
     * {@link PKCS8EncodedKeySpec} -> {@link KeyFactory} builds the {@link RSAPrivateKey}.
     *
     * @param pem location of the PEM file (e.g. {@code file:./keys/private_key.pem})
     * @return the private key
     * @throws IllegalStateException if the file is missing, unreadable or not a PKCS#8 RSA private key -
     *                               thrown at startup so a misconfigured server never starts
     */
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

    /**
     * Reads the RSA public key used to <b>verify</b> token signatures.
     * <p>
     * Same steps as {@link #loadPrivateKey(Resource)}, but public keys are wrapped in the X.509
     * "SubjectPublicKeyInfo" structure, so they need {@link X509EncodedKeySpec}.
     *
     * @param pem location of the PEM file (e.g. {@code file:./keys/public_key.pem})
     * @return the public key
     * @throws IllegalStateException if the file is missing, unreadable or not an X.509 RSA public key
     */
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

    /**
     * Turns PEM text into the raw DER bytes inside it.
     *
     * @param pem  the PEM file
     * @param type the label between "BEGIN" and "-----", e.g. "PRIVATE KEY" or "PUBLIC KEY"
     * @return the Base64-decoded key bytes
     * @throws IllegalStateException if the file does not exist or cannot be read
     */
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
