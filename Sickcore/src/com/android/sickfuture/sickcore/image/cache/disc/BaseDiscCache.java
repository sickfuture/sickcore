package com.android.sickfuture.sickcore.image.cache.disc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import com.android.sickfuture.sickcore.utils.Converter;
import com.android.sickfuture.sickcore.utils.L;

public abstract class BaseDiscCache implements IDiscCache {

	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
	private static final int DEFAULT_COMPRESS_QUALITY = 100;
	private static final String LOG_TAG = BaseDiscCache.class.getSimpleName();

	private File mCacheDir;

	public BaseDiscCache(Context context) {
		if (context == null)
			throw new IllegalArgumentException();
		mCacheDir = context.getCacheDir();
	}

	public BaseDiscCache(File cacheDir) {
		if (cacheDir == null)
			throw new IllegalArgumentException();
		mCacheDir = cacheDir;
	}

	public File getFile(String name) {
		String key = Converter.stringToMD5(name);
		File cacheFile = null;
		cacheFile = new File(mCacheDir, key);
		if (cacheFile.exists()) {
			Log.d(LOG_TAG, "Disk cache hit");
			return cacheFile;
		}
		return cacheFile;
	}

	@Override
	public File put(String name, Bitmap value) {
		String key = Converter.stringToMD5(name);
		FileOutputStream fos = null;
		File cacheFile = null;
		try {
			cacheFile = new File(mCacheDir, key);
			fos = new FileOutputStream(cacheFile);
			value.compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY,
					fos);
			cacheFile.setLastModified(System.currentTimeMillis());
			return cacheFile;
		} catch (IOException e) {
			Log.e(LOG_TAG, "putBitmapToCache - " + e);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ignored) {
			}
		}
		return null;
	}

	@Override
	public boolean clear() {
		File[] files = mCacheDir.listFiles();
		for (File file : files) {
			if (!file.delete()) {
				L.d(LOG_TAG, "failed to delete");
				// return false
			}
		}
		return true;
	}

	protected File getCacheDir() {
		return mCacheDir;
	}

}
