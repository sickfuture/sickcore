package com.android.sickfuture.sickcore.utils;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {
	public static final String LOG_TAG = IOUtils.class.getSimpleName();

	public static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				L.e(LOG_TAG, "Could not close stream");
			}
		}
	}
}
