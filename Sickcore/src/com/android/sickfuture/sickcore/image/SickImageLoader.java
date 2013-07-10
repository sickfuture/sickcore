package com.android.sickfuture.sickcore.image;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import org.apache.http.ParseException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.android.sickfuture.sickcore.BuildConfig;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.http.HttpManager;
import com.android.sickfuture.sickcore.image.cache.ImageCacher;
import com.android.sickfuture.sickcore.image.drawable.RecyclingBitmapDrawable;
import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;
import com.android.sickfuture.sickcore.utils.L;

public class SickImageLoader {

	private static final String LOG_TAG = "ImageLoader";
	public static final String SYSTEM_SERVICE_KEY = "framework:imageloader";

	private int mFadeInTme = 600;
	private boolean mFadeIn = true;
	private Bitmap mPlaceHolderBitmap;

	private static SickImageLoader instance;

	private final Resources mResources;
	private final ImageCacher mImageCacher;
	private final HttpManager mHttpManager;
	private final ImageWorker mImageWorker;

	private final Object mPauseWorkLock = new Object();
	private boolean mPauseWork = false;

	public SickImageLoader(Context context) {
		mResources = context.getResources();
		mImageCacher = ImageCacher.getInstance(context, mResources);
		mHttpManager = HttpManager.getInstance(context);
		mImageWorker = ImageWorker.getInstance(mHttpManager, mImageCacher);
	}

	public static SickImageLoader get(Context context) {
		if (instance == null) {
			instance = new SickImageLoader(context);
		}
		return instance;
	}

	public void setLoadingImage(int resDrawableID) {
		mPlaceHolderBitmap = BitmapFactory.decodeResource(mResources,
				resDrawableID);
	}

	public void setLoadingImage(Bitmap bitmap) {
		mPlaceHolderBitmap = bitmap;
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
		} else if (cancelPotentialDownload(imageView, url)) {
			BitmapAsyncTask bitmapAsyncTask = new BitmapAsyncTask(imageView);
			AsyncBitmapDrawable asyncbitmapDrawable = new AsyncBitmapDrawable(
					mResources, mPlaceHolderBitmap, bitmapAsyncTask);
			imageView.setImageDrawable(asyncbitmapDrawable);
			bitmapAsyncTask.start(url);
		}
	}

	private static boolean cancelPotentialDownload(ImageView imageView,
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
		if (mFadeIn && drawable != null) {
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							drawable });
			if (AndroidVersionsUtils.hasJellyBean()) {
				imageView.setBackground(new BitmapDrawable(mResources,
						mPlaceHolderBitmap));
			} else {
				imageView.setBackgroundDrawable(new BitmapDrawable(mResources,
						mPlaceHolderBitmap));
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
			AsyncTask<String, Void, BitmapDrawable> {

		// private static final int CORE_POOL_SIZE = 3;
		// private static final int MAXIMUM_POOL_SIZE = 15;
		// private static final int KEEP_ALIVE = 5;

		// private final BlockingQueue<Runnable> sWorkQueue = new
		// LinkedBlockingQueue<Runnable>(
		// MAXIMUM_POOL_SIZE);
		// private final ThreadFactory sThreadFactory = new ThreadFactory() {
		// private final AtomicInteger mCount = new AtomicInteger(1);
		//
		// public Thread newThread(Runnable r) {
		// return new Thread(r, "ImageAsyncTask #"
		// + mCount.getAndIncrement());
		// }
		// };
		// private final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(
		// CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
		// TimeUnit.SECONDS, sWorkQueue, sThreadFactory);

		private String mUrl;

		private WeakReference<ImageView> mImageViewReference;

		public BitmapAsyncTask(ImageView imageView) {
			mImageViewReference = new WeakReference<ImageView>(imageView);
		}

		public void start(String url) {
			// custom version of AsyncTask
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		}

		@Override
		protected BitmapDrawable doInBackground(String... params) {
			mUrl = params[0];
			if (TextUtils.isEmpty(mUrl)) {
				if (BuildConfig.DEBUG) {
					L.e(LOG_TAG, "Empty url!");
				}
			}
			synchronized (mPauseWorkLock) {
				while (mPauseWork && !isCancelled()) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
						// TODO Handle exception
					}
				}
			}
			BitmapDrawable bitmapDrawable = null;
			Bitmap bitmap = null;
			if (mImageCacher != null) {
				bitmapDrawable = mImageCacher.getBitmapFromFileCache(mUrl);
			}
			if (bitmapDrawable != null) {
				return bitmapDrawable;
			}
			try {
				if (mHttpManager.isAvalibleInetConnection() && !isCancelled()
						&& getAttachedImageView() != null) {
					bitmap = mImageWorker.loadBitmap(mUrl, 500, 500);
				}
				if (bitmap != null) {
					if (AndroidVersionsUtils.hasHoneycomb()
							&& bitmap.getConfig() != null) {
						bitmapDrawable = new BitmapDrawable(mResources, bitmap);
					} else {
						bitmapDrawable = new RecyclingBitmapDrawable(
								mResources, bitmap);
					}
					mImageCacher.put(mUrl, bitmapDrawable);
				}
			} catch (MalformedURLException e) {
				// TODO Handle exception
			} catch (BadRequestException e) {
				// TODO Handle exception
			} catch (ParseException e) {
				// TODO Handle exception
			} catch (IOException e) {
				// TODO Handle exception
			}
			return bitmapDrawable;
		}

		@Override
		protected void onPostExecute(BitmapDrawable result) {
			if (isCancelled()) {
				result = null;
			}
			if (mImageViewReference != null) {
				ImageView imageView = mImageViewReference.get();
				BitmapAsyncTask bitmapDownloaderTask = getImageLoaderTask(imageView);
				// Change bitmap only if this process is still associated with
				// it
				if (this == bitmapDownloaderTask) {
					if (imageView != null) {
						setImageDrawable(imageView, result);
					}
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

// TODO remove comments
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