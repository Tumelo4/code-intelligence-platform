package com.codeintel.infrastructure.repository;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class PemPrivateKeyLoader {
    private PemPrivateKeyLoader() {
    }

    public static PrivateKey loadPkcs8(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("GitHub App private key is missing");
        }
        String normalized = pem.replace("\\n", "\n");
        boolean pkcs1 = normalized.contains("-----BEGIN RSA PRIVATE KEY-----");
        String encoded = normalized.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encoded);
            if (pkcs1) {
                keyBytes = wrapPkcs1AsPkcs8(keyBytes);
            }
            return KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("GitHub App private key must be PKCS#8 RSA PEM", exception);
        }
    }

    private static byte[] wrapPkcs1AsPkcs8(byte[] pkcs1) {
        byte[] version = {0x02, 0x01, 0x00};
        byte[] rsaAlgorithm = {0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
                (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00};
        byte[] privateKey = tagged((byte) 0x04, pkcs1);
        return tagged((byte) 0x30, concatenate(version, rsaAlgorithm, privateKey));
    }

    private static byte[] tagged(byte tag, byte[] value) {
        return concatenate(new byte[]{tag}, encodedLength(value.length), value);
    }

    private static byte[] encodedLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        int bytes = 0;
        int remaining = length;
        while (remaining > 0) {
            bytes++;
            remaining >>>= 8;
        }
        byte[] encoded = new byte[bytes + 1];
        encoded[0] = (byte) (0x80 | bytes);
        for (int index = bytes; index > 0; index--) {
            encoded[index] = (byte) length;
            length >>>= 8;
        }
        return encoded;
    }

    private static byte[] concatenate(byte[]... values) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] value : values) {
            output.writeBytes(value);
        }
        return output.toByteArray();
    }
}
