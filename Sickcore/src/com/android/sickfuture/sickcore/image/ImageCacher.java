package com.android.sickfuture.sickcore.image;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.android.sickfuture.sickcore.image.cache.disc.LimitedDiscCache;
import com.android.sickfuture.sickcore.image.drawable.RecyclingBitmapDrawable;
import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;

public class ImageCacher {

	protected static final String LOG_TAG = ImageCacher.class.getSimpleName();

	public static final String SYSTEM_SERVICE_KEY = "sickcore:imagecacher";

	private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
	private static final boolean DEFAULT_DISK_CACHE_ENABLED = false;

	private boolean mCacheOnMemory = DEFAULT_MEM_CACHE_ENABLED;
	private boolean mCacheOnDisc = DEFAULT_DISK_CACHE_ENABLED;

	private static final int DEFAULT_MEMORY_CACHE_LIMIT = 2 * 1024 * 1024;// 2MB
	private int cacheSize = DEFAULT_MEMORY_CACHE_LIMIT;

	private static final int DISC_CACHE_LIMIT = 10 * 1024 * 1024; // 10MB

	private LruCache<String, BitmapDrawable> mStorage;

	private Set<SoftReference<Bitmap>> mReusableBitmaps;

	private Resources mResources;
	private File mCacheDir;

	private LimitedDiscCache mDiscCache;

	private int memClass;

	protected ImageCacher(Context context) {
		mResources = context.getResources();
		init(context);
	}

	private void init(Context context) {
		mCacheDir = context.getCacheDir();
		if (AndroidVersionsUtils.hasHoneycomb()) {
			mReusableBitmaps = Collections
					.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
		}
		memClass = ((ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		// cacheSize = 1024 * 1024 * memClass / 4;
		mStorage = new LruCache<String, BitmapDrawable>(cacheSize) {
			@Override
			protected int sizeOf(String key, BitmapDrawable value) {
				return value.getBitmap().getRowBytes()
						* value.getBitmap().getHeight();
			}

			@Override
			protected void entryRemoved(boolean evicted, String key,
					BitmapDrawable oldValue, BitmapDrawable newValue) {
				if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
					((RecyclingBitmapDrawable) oldValue).setIsCached(false);
				}
				if (AndroidVersionsUtils.hasHoneycomb()) {
					if (mReusableBitmaps.size() < 50) {
						mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue
								.getBitmap()));
					}
				}
			}
		};
		mDiscCache = new LimitedDiscCache(mCacheDir, DISC_CACHE_LIMIT);
	}

	public BitmapDrawable getBitmapFromFileCache(String url) {
		Bitmap result = mDiscCache.get(url);
		BitmapDrawable drawable = null;
		if (result != null && mCacheOnMemory) {
			if (AndroidVersionsUtils.hasHoneycomb()) {
				drawable = new BitmapDrawable(mResources, result);
			} else {
				drawable = new RecyclingBitmapDrawable(mResources, result);
			}
			putBitmapToMemoryCache(url, drawable);
		}
		return drawable;
	}

	public BitmapDrawable getBitmapFromMemoryCache(String key) {
		if (mStorage != null) {
			return mStorage.get(key);
		} else {
			throw new NullPointerException("LruCache object is null!!");
		}
	}

	public void put(String url, BitmapDrawable value) {
		if (!TextUtils.isEmpty(url) && value != null) {
			if (mCacheOnMemory)
				putBitmapToMemoryCache(url, value);
			if (mCacheOnDisc)
				mDiscCache.put(url, value.getBitmap());
		}

	}

	public void putBitmapToMemoryCache(String key, BitmapDrawable value) {
		if (key == null || value == null) {
			return;
		}
		if (mStorage != null && getBitmapFromMemoryCache(key) == null) {
			if (RecyclingBitmapDrawable.class.isInstance(value)) {
				((RecyclingBitmapDrawable) value).setIsCached(true);
			}
			mStorage.put(key, value);
		}
	}

	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
		Bitmap bitmap = null;
		if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
			synchronized (mReusableBitmaps) {
				final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps
						.iterator();
				Bitmap item = null;
				while (iterator.hasNext()) {
					item = iterator.next().get();

					if (null != item && item.isMutable()) {
						if (canUseForInBitmap(item, options)) {
							bitmap = item;
							iterator.remove();
							break;
						}
					} else {
						iterator.remove();
					}
				}
			}
		}
		return bitmap;
	}

	private static boolean canUseForInBitmap(Bitmap candidate,
			BitmapFactory.Options targetOptions) {
		if (targetOptions.inSampleSize <= 0) {
			return false;
		}
		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;
		return candidate.getWidth() == width && candidate.getHeight() == height
				&& candidate.getConfig() == targetOptions.inPreferredConfig;
	}

}
