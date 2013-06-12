package com.android.sickfuture.sickcore.image;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.android.sickfuture.sickcore.BuildConfig;
import com.android.sickfuture.sickcore.asynctask.CustomExecutorAsyncTask;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.http.HttpManager;
import com.android.sickfuture.sickcore.image.cache.ImageCacher;
import com.android.sickfuture.sickcore.utils.L;

public class SuperImageLoader {

	private static final String LOG_TAG = "ImageLoader";

	private static final int FADE_IN_TIME = 600;
	private boolean mFadeInBitmap = true;

	private static volatile SuperImageLoader instance;

	private final Resources mResources;
	private final ImageCacher mImageCacher;
	private final HttpManager mHttpManager;
	private final ImageWorker mImageWorker;

	private Bitmap mLoadingBitmap;

	private final Object mPauseWorkLock = new Object();
	private boolean mPauseWork = false;

	private SuperImageLoader(Context context) {
		mImageCacher = ImageCacher.getInstance(context);
		mResources = context.getResources();
		mHttpManager = HttpManager.getInstance(context);
		mImageWorker = ImageWorker.getInstance(context);
	}

	public static SuperImageLoader getInstance(Context context) {
		SuperImageLoader localInstance = instance;
		if (localInstance == null) {
			synchronized (SuperImageLoader.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new SuperImageLoader(context);
				}
			}
		}
		return localInstance;
	}

	public void setLoadingImage(int resDrawableID) {
		mLoadingBitmap = BitmapFactory
				.decodeResource(mResources, resDrawableID);
	}

	public void setLoadingImage(Bitmap bitmap) {
		mLoadingBitmap = bitmap;
	}

	public void setFadeInBitmap(boolean fadeIn) {
		mFadeInBitmap = fadeIn;
	}

	public void loadBitmap(ImageView imageView, String url,
			final boolean cacheOnDiskMemory) {
		if (url == null || url.equals("")) {
			if (BuildConfig.DEBUG) {
				L.e(LOG_TAG, "empty or null url");
			}
			return;
		}

		Bitmap bitm = null;
		bitm = mImageCacher.getBitmapFromMemoryCache(url);
		if (bitm != null) {
			setImageDrawable(imageView, new BitmapDrawable(mResources, bitm));
		} else if (cancelPotentialDownLoad(imageView, url)) {
			BitmapAsyncTask bitmapAsyncTask = new BitmapAsyncTask(imageView);
			AsyncBitmapDrawable bitmapDrawable = new AsyncBitmapDrawable(
					mResources, mLoadingBitmap, bitmapAsyncTask);
			imageView.setImageDrawable(bitmapDrawable);
			bitmapAsyncTask.start(url);
		}
	}

	private static boolean cancelPotentialDownLoad(ImageView imageView,
			String url) {
		BitmapAsyncTask bitmapAsyncTask = getImageLoaderTask(imageView);
		if (bitmapAsyncTask != null) {
			String bitmapUrl = bitmapAsyncTask.mUrl;
			if (bitmapUrl == null || !bitmapUrl.equals(url)) {
				bitmapAsyncTask.cancel(true);
			} else {
				// The same URL is already being downloaded.
				return false;
			}
		}
		return true;
	}

	private static BitmapAsyncTask getImageLoaderTask(ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncBitmapDrawable) {
				AsyncBitmapDrawable downloadedDrawable = (AsyncBitmapDrawable) drawable;
				return downloadedDrawable.getLoaderTask();
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private void setImageDrawable(ImageView imageView, Drawable drawable) {
		if (mFadeInBitmap) {
			// Transition drawable with a transparent drawable and the final
			// drawable
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							drawable });
			// Set background to loading bitmap
			imageView.setBackgroundDrawable(new BitmapDrawable(mResources,
					mLoadingBitmap));
			imageView.setImageDrawable(td);
			td.startTransition(FADE_IN_TIME);
		} else {
			imageView.setImageDrawable(drawable);
		}
	}

	/**
	 * Pause any ongoing background work. This can be used as a temporary
	 * measure to improve performance. For example background work could be
	 * paused when a ListView or GridView is being scrolled using a
	 * {@link android.widget.AbsListView.OnScrollListener} to keep scrolling
	 * smooth.
	 * <p>
	 * If work is paused, be sure setPauseWork(false) is called again before
	 * your fragment or activity is destroyed (for example during
	 * {@link android.app.Activity#onPause()}), or there is a risk the
	 * background thread will never finish.
	 */
	public void setPauseWork(boolean pauseWork) {
		synchronized (mPauseWorkLock) {
			mPauseWork = pauseWork;
			if (!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

	public class BitmapAsyncTask extends
			CustomExecutorAsyncTask<String, Void, Object> {

		private static final int CORE_POOL_SIZE = 3;
		private static final int MAXIMUM_POOL_SIZE = 15;
		private static final int KEEP_ALIVE = 5;

		private final BlockingQueue<Runnable> sWorkQueue = new LinkedBlockingQueue<Runnable>(
				MAXIMUM_POOL_SIZE);
		private final ThreadFactory sThreadFactory = new ThreadFactory() {
			private final AtomicInteger mCount = new AtomicInteger(1);

			public Thread newThread(Runnable r) {
				return new Thread(r, "ImageAsyncTask #"
						+ mCount.getAndIncrement());
			}
		};
		private final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(
				CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
				TimeUnit.SECONDS, sWorkQueue, sThreadFactory);

		private String mUrl;

		private WeakReference<ImageView> mImageViewReference;

		public BitmapAsyncTask(ImageView imageView) {
			mImageViewReference = new WeakReference<ImageView>(imageView);
		}

		public void start(String url) {
			execute(mExecutor, url);
		}

		@Override
		protected Object doInBackground(String... params) {
			mUrl = params[0];
			if (TextUtils.isEmpty(mUrl)) {
				return new IllegalArgumentException("Empty url!");
			}
			synchronized (mPauseWorkLock) {
				while (mPauseWork && !isCancelled()) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
						return e;
					}
				}
			}
			Bitmap bitmap = null;
			try {
				bitmap = mImageCacher.getBitmapFromFileCache(mUrl);
				if (bitmap != null) {
					mImageCacher.putBitmapToMemoryCache(mUrl, bitmap);
				}
				if (bitmap != null) {
					return bitmap;
				}
				try {
					if (mHttpManager.isAvalibleInetConnection()) {
						bitmap = mImageWorker.loadBitmap(mUrl, 500, 500);
					}
					if (bitmap != null) {
						mImageCacher.putBitmapToCache(mUrl, bitmap, false);
					}
				} catch (MalformedURLException e) {
					return e;
				} catch (BadRequestException e) {
					return e;
				}
			} catch (IOException e) {
				return e;
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Object result) {
			if (mImageViewReference != null) {
				ImageView imageView = mImageViewReference.get();
				BitmapAsyncTask bitmapDownloaderTask = getImageLoaderTask(imageView);
				// Change bitmap only if this process is still associated with
				// it
				if (this == bitmapDownloaderTask) {
					setImageDrawable(imageView, new BitmapDrawable(mResources,
							(Bitmap) result));
				}
			}
		}
	}
}