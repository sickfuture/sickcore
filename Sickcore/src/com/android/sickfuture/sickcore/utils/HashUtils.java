package com.android.sickfuture.sickcore.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

	public static long generateId(Object... value) {
		// String value to be converted
		try {
			StringBuilder builder = new StringBuilder();
			for (Object s : value) {
				builder.append(String.valueOf(s));
			}
			MessageDigest md = MessageDigest.getInstance("sha-1");

			// convert the string value to a byte array and pass it into the
			// hash algorithm
			md.update(builder.toString().getBytes());

			// retrieve a byte array containing the digest
			byte[] hashValBytes = md.digest();

			long hashValLong = 0;

			// create a long value from the byte array
			for (int i = 0; i < 8; i++) {
				hashValLong |= ((long) (hashValBytes[i]) & 0x0FF) << (8 * i);
			}
			return hashValLong;
		} catch (NoSuchAlgorithmException e) {
			return 0l;
		}
	}
}
