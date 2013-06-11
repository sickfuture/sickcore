package com.android.sickfuture.sickcore.image.cache.disc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.sickfuture.sickcore.utils.Converter;
import com.android.sickfuture.sickcore.utils.L;

public class LimitedDiscCache extends BaseDiscCache {

	private static final int MIN_NORMAL_CACHE_SIZE_IN_MB = 2;
	private static final int MIN_NORMAL_CACHE_SIZE = MIN_NORMAL_CACHE_SIZE_IN_MB * 1024 * 1024;
	private static final String LOG_TAG = LimitedDiscCache.class
			.getSimpleName();
	private AtomicInteger mCacheSize;
	private int mCacheLimit;

	private Map<String, Long> LRUList = Collections
			.synchronizedMap(new HashMap<String, Long>());

	public LimitedDiscCache(File dir, int limit) {
		super(dir);
		mCacheLimit = (limit > MIN_NORMAL_CACHE_SIZE) ? limit
				: MIN_NORMAL_CACHE_SIZE;
		mCacheSize = new AtomicInteger();
		initCalculateCacheSize();
	}

	private void initCalculateCacheSize() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int size = 0;
				File[] cachedFiles = getCacheDir().listFiles();
				if (cachedFiles != null) {
					for (File cachedFile : cachedFiles) {
						if (!cachedFile.isDirectory()) {
							size += cachedFile.length();
							L.d(cachedFile.lastModified() + "",
									cachedFile.getName());
							LRUList.put(cachedFile.getName(),
									cachedFile.lastModified());
						}
					}
					mCacheSize.set(size);
					// L.d(LOG_TAG, "init cache size=" + size +
					// ", lru list size="
					// + LRUList.size());
				}
			}
		}).start();
	}

	// update in lrulist
	@Override
	public Bitmap get(String name) {
		String key = Converter.stringToMD5(name);
		File file = super.getFile(name);
		long update = System.currentTimeMillis();
		if (LRUList.remove(key) != null) {
			LRUList.put(key, update);
			// L.d(LOG_TAG, "image updated");
		} else
			// L.w(LOG_TAG, "update failure");
			file.setLastModified(update);
		Bitmap bitmap = null;
		FileInputStream fis = null;
		try {
			if (file.exists()) {
				fis = new FileInputStream(file);
				bitmap = BitmapFactory.decodeFileDescriptor(fis.getFD());
				Log.d(LOG_TAG, "Disk cache hit");
			}
		} catch (FileNotFoundException e) {
			// Ignored, because not cached yet
		} catch (IOException e) {
			// TODO do smth
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ignored) {
				}
			}
		}
		return bitmap;
	}

	// add to lrulist
	@Override
	public File put(String name, Bitmap value) {
		File cached = super.put(name, value);
		String key = Converter.stringToMD5(name);
		int fileSize = (int) cached.length();
		long currSize = mCacheSize.get();
		while (currSize + fileSize > mCacheLimit) {
			int freed = removeLastUsed();
			L.d(LOG_TAG, "freed=" + freed);
			currSize -= freed;
			if (freed == 0)
				break;
			else
				mCacheSize.addAndGet(-freed);
		}
		LRUList.put(key, cached.lastModified());
		mCacheSize.addAndGet(fileSize);
		// L.d(LOG_TAG, "putted, cachesize=" + mCacheSize.get());
		return cached;
	}

	private int removeLastUsed() {
		if (LRUList.size() == 0) {
			// L.d(LOG_TAG, "lrulist empty when remove last used");
			return 0;
		}
		long oldest = -1;
		String oldestKey = null;
		Set<Entry<String, Long>> entries = LRUList.entrySet();
		synchronized (LRUList) {
			for (Entry<String, Long> entry : entries) {
				if (oldest == -1) {
					oldest = entry.getValue();
					oldestKey = entry.getKey();
				} else if (oldest > entry.getValue()) {
					oldest = entry.getValue();
					oldestKey = entry.getKey();
				}
			}
		}
		if (oldestKey == null) {
			// L.d(LOG_TAG, "oldest null");
			return 0;
		}
		File toRemove = new File(getCacheDir(), oldestKey);
		// L.d(LOG_TAG, "oldest is " + oldestKey);
		if (!toRemove.exists()) {
			LRUList.remove(oldestKey);
			// L.d(LOG_TAG, "doesnt exist " + toRemove.getName());
		}
		int removedSize = (int) toRemove.length();
		// L.d(LOG_TAG, "to remove file size=" + removedSize + ", lru size="
		// + LRUList.size());
		toRemove.delete();
		LRUList.remove(oldestKey);
		return removedSize;

	}

	@Override
	public boolean clear() {
		// TODO Auto-generated method stub
		return super.clear();
	}

}
