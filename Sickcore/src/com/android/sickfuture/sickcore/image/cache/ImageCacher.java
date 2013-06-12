package com.android.sickfuture.sickcore.image.cache;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Set;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.sickfuture.sickcore.context.ContextHolder;
import com.android.sickfuture.sickcore.image.cache.disc.LimitedDiscCache;

public class ImageCacher {

	Set<SoftReference<Bitmap>> mReusableBitmaps;

	protected static final String LOG_TAG = ImageCacher.class.getSimpleName();

	private static ImageCacher instance;

	private Context mContext;

	private LruCache<String, Bitmap> mStorage;

	private File mCacheDir;

	private LimitedDiscCache mDiscCache;

	private static final int DISC_CACHE_LIMIT = 20 * 1024 * 1024;

	private final int memClass = ((ActivityManager) ContextHolder.getInstance()
			.getContext().getSystemService(Context.ACTIVITY_SERVICE))
			.getMemoryClass();

	private final int cacheSize = 1024 * 1024 * memClass / 4;

	public static ImageCacher getInstance(Context context) {
		if (instance == null) {
			instance = new ImageCacher(context);
		}
		return instance;
	}

	private ImageCacher(Context context) {
		init(context);
	}

	private void init(Context context) {
		mContext = context;
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
	
	/**
	 * @param options
	 *            - BitmapFactory.Options with out* options populated
	 * @return Bitmap that case be used for inBitmap
	 */
	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
		Bitmap bitmap = null;
		synchronized (mReusableBitmaps) {
			if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
				final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps
						.iterator();
				Bitmap item;

				while (iterator.hasNext()) {
					SoftReference<Bitmap> reference = iterator.next();
					item = reference.get();

					if (null != item && item.isMutable()) {
						// Check to see it the item can be used for inBitmap
						if (canUseForInBitmap(item, options)) {
							bitmap = item;

							// Remove from reusable set so it can't be used
							// again
							iterator.remove();
							break;
						}
					} else {
						// Remove from the set if the reference has been
						// cleared.
						iterator.remove();
					}
				}
			}
		}
		return bitmap;
	}

	private static boolean canUseForInBitmap(Bitmap candidate,
			BitmapFactory.Options targetOptions) {
		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;

		return candidate.getWidth() == width && candidate.getHeight() == height;
	}
}


