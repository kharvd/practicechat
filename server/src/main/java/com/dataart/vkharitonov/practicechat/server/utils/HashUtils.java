package com.dataart.vkharitonov.practicechat.server.utils;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;
import java.util.Random;

public final class HashUtils {

    private static Random random = new SecureRandom();

    public static String hash(String password, String salt) {
        return Hashing.sha512().hashUnencodedChars(password + salt).toString();
    }

    public static String newSalt() {
        byte[] saltBytes = new byte[24];
        random.nextBytes(saltBytes);
        return BaseEncoding.base16().encode(saltBytes);
    }
}
