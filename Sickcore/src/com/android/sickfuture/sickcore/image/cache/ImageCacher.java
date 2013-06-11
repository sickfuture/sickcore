package com.android.sickfuture.sickcore.image.cache;

import java.io.File;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.sickfuture.sickcore.context.ContextHolder;
import com.android.sickfuture.sickcore.image.cache.disc.LimitedDiscCache;

public class ImageCacher {

	protected static final String LOG_TAG = ImageCacher.class.getSimpleName();

	private static ImageCacher instance;

	private Context mContext = ContextHolder.getInstance().getContext();

	private LruCache<String, Bitmap> mStorage;

	private File mCacheDir;

	private LimitedDiscCache mDiscCache;

	private static final int DISC_CACHE_LIMIT = 20 * 1024 * 1024;

	private final int memClass = ((ActivityManager) ContextHolder.getInstance()
			.getContext().getSystemService(Context.ACTIVITY_SERVICE))
			.getMemoryClass();

	private final int cacheSize = 1024 * 1024 * memClass / 4;

	public static ImageCacher getInstance() {
		if (instance == null) {
			instance = new ImageCacher();
		}
		return instance;
	}

	private ImageCacher() {
		init();
	}

	private void init() {
		mStorage = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight();
			}

			@Override
			protected void entryRemoved(boolean evicted, String key,
					Bitmap oldValue, Bitmap newValue) {
				super.entryRemoved(evicted, key, oldValue, newValue);
				if (evicted) {
					oldValue.recycle();
					Log.d(LOG_TAG, "recycle");
					System.gc();
					oldValue = null;
				}
			}
		};
		mCacheDir = mContext.getCacheDir();
		mDiscCache = new LimitedDiscCache(mCacheDir, DISC_CACHE_LIMIT);
	}

	public Bitmap getBitmapFromFileCache(String url) {
		// String key = Converter.stringToMD5(url);
		// Bitmap bitmap = null;
		// FileInputStream fis = null;
		// File cacheFile = null;
		// try {
		// cacheFile = new File(mCacheDir, key);
		// if (cacheFile.exists()) {
		// fis = new FileInputStream(cacheFile);
		// bitmap = BitmapFactory.decodeFileDescriptor(fis.getFD());
		// Log.d(LOG_TAG, "Disk cache hit");
		// }
		// } catch (FileNotFoundException e) {
		// // Ignored, because already not cashed
		// } catch (IOException e) {
		// // TODO do smth
		// } finally {
		// if (fis != null) {
		// try {
		// fis.close();
		// } catch (IOException e) {
		// // ignored if already closed
		// }
		// }
		// }
		// return bitmap;
		return mDiscCache.get(url);
	}

	public void putBitmapToCache(String url, Bitmap value,
			boolean cacheOnDiskMemory) {
		if (url == null || value == null) {
			return;
		}
		putBitmapToMemoryCache(url, value);
		if (cacheOnDiskMemory) {
			mDiscCache.put(url, value);
		}
		// String key = Converter.stringToMD5(url);
		// FileOutputStream fos = null;
		// File cacheFile = null;
		// try {
		// cacheFile = new File(mCacheDir, key);
		// fos = new FileOutputStream(cacheFile);
		// value.compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY,
		// fos);
		// } catch (IOException e) {
		// Log.e(LOG_TAG, "putBitmapToCache - " + e);
		// } finally {
		// try {
		// if (fos != null) {
		// fos.close();
		// }
		// } catch (IOException e) {
		// }
		// }
	}

	public void putBitmapToMemoryCache(String key, Bitmap value) {
		if (mStorage != null) {
			mStorage.put(key, value);
		}
	}

	public Bitmap getBitmapFromMemoryCache(String key) {
		if (mStorage != null) {
			return mStorage.get(key);
		} else {
			throw new NullPointerException("LruCache object is null!!");
		}
	}
}
