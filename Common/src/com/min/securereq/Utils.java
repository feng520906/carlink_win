package com.min.securereq;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/**
 * Created by min on 14/12/4.
 * <p/>
 * Packing & unpacking utils.
 * <p/>
 * Message format:
 * version(0), digest(8~15), encrypted_data(16~n)
 */
@SuppressWarnings("UnusedDeclaration")
public class Utils {
    static final int HEAD_LEN = 8;
    static final int DIGEST_LEN = 20;
    static final int MAX_COMPRESS_SIZE = 1024;

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] pack_msg(byte[] aesKey, byte[] hmacKey, byte[] data) {
        try {
            // head
            byte[] head_data = new byte[HEAD_LEN];
            Arrays.fill(head_data, (byte) 0);
            head_data[0] = 1;

            byte[] compressed = data;
            if (data.length >= MAX_COMPRESS_SIZE) {
                // compress
                head_data[1] = 1;
                compressed = gzip(data);
            }

            // encrypt
            byte[] encrypted = encrypt_aes_pkcs5(aesKey, compressed, 0, compressed.length);

            // digest
            byte[] digest = calc_hmac_sha1_digest(hmacKey, encrypted, 0, encrypted.length);

            // create return buffer
            byte[] ret = new byte[head_data.length + digest.length + encrypted.length];

            // put head
            System.arraycopy(head_data, 0, ret, 0, head_data.length);

            // put digest
            System.arraycopy(digest, 0, ret, head_data.length, digest.length);

            // put encrypted data
            System.arraycopy(encrypted, 0, ret, head_data.length + digest.length, encrypted.length);

            return ret;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] unpack_msg(byte[] aesKey, byte[] hmacKey, byte[] data) throws NoSuchPaddingException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, IOException, InvalidResponseException {
        if (!check_digest(hmacKey, data)) {
            throw new InvalidResponseException();
        }

        byte[] decrypted_data = decrypt_aes_pkcs5(aesKey, data, HEAD_LEN + DIGEST_LEN, data.length - HEAD_LEN - DIGEST_LEN);

        byte[] ret = decrypted_data;
        if (data[1] == 1) {
            ret = gunzip(decrypted_data);
        }

        return ret;
    }

    private static boolean check_digest(byte[] hmacKey, byte[] data) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] digest = new byte[DIGEST_LEN];
        System.arraycopy(data, HEAD_LEN, digest, 0, digest.length);

        byte[] my_digest = calc_hmac_sha1_digest(hmacKey, data,
                HEAD_LEN + DIGEST_LEN, data.length - HEAD_LEN - DIGEST_LEN);

        return Arrays.equals(digest, my_digest);
    }

    private static byte[] calc_hmac_sha1_digest(byte[] hmacKey, byte[] data, int offset, int len) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(hmacKey, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        mac.update(data, offset, len);
        return mac.doFinal();
    }

    private static byte[] decrypt_aes_pkcs5(byte[] key, byte[] data, int offset, int length) throws NoSuchPaddingException, NoSuchAlgorithmException,
            BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        c.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(data, offset, 16));
        return c.doFinal(data, offset + 16, length - 16);
    }

    private static byte[] encrypt_aes_pkcs5(byte[] key, byte[] data, int offset, int length) throws NoSuchPaddingException, NoSuchAlgorithmException,
            BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        // build the initialization vector (randomly).
        SecureRandom random = new SecureRandom();
        byte iv[] = new byte[16];
        // generate random 16 byte IV AES is always 16bytes
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        c.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted_data = c.doFinal(data, offset, length);

        byte[] ret = new byte[encrypted_data.length + iv.length];
        System.arraycopy(iv, 0, ret, 0, iv.length);
        System.arraycopy(encrypted_data, 0, ret, iv.length, encrypted_data.length);
        return ret;
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(data);
        gos.close();
        os.close();
        return os.toByteArray();
    }

    private static byte[] gunzip(byte[] out) throws IOException {
        return gunzip(out, 0, out.length);
    }

    private static byte[] gunzip(byte[] compressed, int offset, int length) throws IOException {
        java.io.ByteArrayInputStream byteArrayIn = new java.io.ByteArrayInputStream(compressed, offset, length);
        java.util.zip.GZIPInputStream gzipIn = new java.util.zip.GZIPInputStream(byteArrayIn);
        java.io.ByteArrayOutputStream byteArrayOut = new java.io.ByteArrayOutputStream();

        int readSize = 0;
        byte buf[] = new byte[1024];
        while (readSize >= 0) {
            readSize = gzipIn.read(buf, 0, buf.length);
            if (readSize > 0) {
                byteArrayOut.write(buf, 0, readSize);
            }
        }
        return byteArrayOut.toByteArray();
    }
}
