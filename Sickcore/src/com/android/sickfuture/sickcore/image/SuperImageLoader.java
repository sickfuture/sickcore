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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.android.sickfuture.sickcore.BuildConfig;
import com.android.sickfuture.sickcore.asynctask.CustomExecutorAsyncTask;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.http.HttpManager;
import com.android.sickfuture.sickcore.image.cache.ImageCacher;
import com.android.sickfuture.sickcore.image.drawable.RecyclingBitmapDrawable;
import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;
import com.android.sickfuture.sickcore.utils.L;

public class SuperImageLoader {

	private static final String LOG_TAG = "ImageLoader";

	private int mFadeInTme = 600;
	private boolean mFadeIn = true;
	private Bitmap mLoadingBitmap;

	private static volatile SuperImageLoader instance;

	private final Resources mResources;
	private final ImageCacher mImageCacher;
	private final HttpManager mHttpManager;
	private final ImageWorker mImageWorker;

	private final Object mPauseWorkLock = new Object();
	private boolean mPauseWork = false;

	private SuperImageLoader(Context context) {
		mResources = context.getResources();
		mImageCacher = ImageCacher.getInstance(context, mResources);
		mHttpManager = HttpManager.getInstance(context);
		mImageWorker = ImageWorker.getInstance(mHttpManager, mImageCacher);
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
		mFadeIn = fadeIn;
	}

	public void loadBitmap(ImageView imageView, String url) {
		if (TextUtils.isEmpty(url)) {
			if (BuildConfig.DEBUG) {
				L.e(LOG_TAG, "empty or null url");
			}
			return;
		}
		BitmapDrawable bitmapDrawable = mImageCacher
				.getBitmapFromMemoryCache(url);
		if (bitmapDrawable != null) {
			setImageDrawable(imageView, bitmapDrawable);
		} else if (cancelPotentialDownLoad(imageView, url)) {
			BitmapAsyncTask bitmapAsyncTask = new BitmapAsyncTask(imageView);
			AsyncBitmapDrawable asyncbitmapDrawable = new AsyncBitmapDrawable(
					mResources, mLoadingBitmap, bitmapAsyncTask);
			imageView.setImageDrawable(asyncbitmapDrawable);
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
				if (BuildConfig.DEBUG) {
					Log.d(LOG_TAG, "cancelPotentialWork - cancelled work for "
							+ url);
				}
			} else {
				// The same URL is already being downloaded.
				return false;
			}
		}
		return true;
	}

	private static BitmapAsyncTask getImageLoaderTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncBitmapDrawable) {
				final AsyncBitmapDrawable downloadedDrawable = (AsyncBitmapDrawable) drawable;
				return downloadedDrawable.getLoaderTask();
			}
		}
		return null;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void setImageDrawable(ImageView imageView, Drawable drawable) {
		if (mFadeIn) {
			// Transition drawable with a transparent drawable and the final
			// drawable
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							drawable });
			// Set background to loading bitmap
			if (AndroidVersionsUtils.hasJellyBean()) {
				imageView.setBackground(new BitmapDrawable(mResources,
						mLoadingBitmap));
			} else {
				imageView.setBackgroundDrawable(new BitmapDrawable(mResources,
						mLoadingBitmap));
			}
			imageView.setImageDrawable(td);
			td.startTransition(mFadeInTme);
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
			BitmapDrawable bitmapDrawable = null;
			Bitmap bitmap = null;
			try {
				bitmapDrawable = mImageCacher.getBitmapFromFileCache(mUrl);
				if (bitmapDrawable != null) {
					return bitmapDrawable;
				}
				try {
					if (mHttpManager.isAvalibleInetConnection()
							&& !isCancelled() && getAttachedImageView() != null) {
						bitmap = mImageWorker.loadBitmap(mUrl, 500, 500);
					}
					if (bitmap != null) {
						// TODO fix inBitmap bug
						// if (AndroidVersionsUtils.hasHoneycomb()) {
						// bitmapDrawable = new BitmapDrawable(mResources,
						// bitmap);
						// }else{
						bitmapDrawable = new RecyclingBitmapDrawable(
								mResources, bitmap);
						// }
						mImageCacher.put(mUrl, bitmapDrawable);
					}
				} catch (MalformedURLException e) {
					return e;
				} catch (BadRequestException e) {
					return e;
				}
			} catch (IOException e) {
				return e;
			}
			return bitmapDrawable;
		}

		@Override
		protected void onPostExecute(Object result) {
			if (isCancelled()) {
				result = null;
			}
			if (result == null) {
				return;
			}
			if (!(BitmapDrawable.class.isInstance(result))) {
				if (BuildConfig.DEBUG) {
					L.e(LOG_TAG, ((Throwable) result).getMessage());
				}
				return;
			}
			if (mImageViewReference != null) {
				ImageView imageView = mImageViewReference.get();
				BitmapAsyncTask bitmapDownloaderTask = getImageLoaderTask(imageView);
				// Change bitmap only if this process is still associated with
				// it
				if (this == bitmapDownloaderTask) {
					setImageDrawable(imageView, (BitmapDrawable) result);
				}
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			synchronized (mPauseWorkLock) {
				mPauseWorkLock.notifyAll();
			}
		}

		private ImageView getAttachedImageView() {
			final ImageView imageView = mImageViewReference.get();
			final BitmapAsyncTask bitmapWorkerTask = getImageLoaderTask(imageView);

			if (this == bitmapWorkerTask) {
				return imageView;
			}

			return null;
		}
	}
}

// private SuperImageLoader(Builder builder) {
// mImageCacher = builder.mImageCacher;
// mResources = builder.mResources;
// mHttpManager = builder.mHttpManager;
// mImageWorker = builder.mImageWorker;
// mFadeIn = builder.mFadeIn;
// mFadeInTme = builder.mFadeInTime;
// mLoadingBitmap = builder.mLoadingBitmap;
// }
//
// public static class Builder {
//
// private boolean DEFAULT_FADE_IN = true;
// private boolean mFadeIn = DEFAULT_FADE_IN;
// private static final int DEFAULT_FADE_IN_TIME = 600;
// private int mFadeInTime = DEFAULT_FADE_IN_TIME;
// private Bitmap mLoadingBitmap;
//
// private final Resources mResources;
// private final ImageCacher mImageCacher;
// private final HttpManager mHttpManager;
// private final ImageWorker mImageWorker;
//
// public Builder(Context context) {
// mImageCacher = ImageCacher.getInstance(context);
// mResources = context.getResources();
// mHttpManager = HttpManager.getInstance(context);
// mImageWorker = ImageWorker.getInstance(context);
// }
//
// public Builder enableFadeIn(boolean enable, int... duration) {
// mFadeIn = enable;
// if (duration.length > 0 && duration[0] > 0) {
// mFadeInTime = duration[0];
// }
// return this;
// }
//
// public void setLoadingImage(int resDrawableID) {
// mLoadingBitmap = BitmapFactory.decodeResource(mResources,
// resDrawableID);
// }
//
// public void setLoadingImage(Bitmap bitmap) {
// mLoadingBitmap = bitmap;
// }
//
// public SuperImageLoader build() {
// // TODO if create a few builders - creates few SuperImageLoaders
// // instances. Should use getInstance(context)
// return new SuperImageLoader(this);
// }
//
// }