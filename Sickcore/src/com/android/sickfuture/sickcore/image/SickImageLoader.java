package com.android.sickfuture.sickcore.image;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.android.sickfuture.sickcore.app.AppHelper.IAppServiceKey;
import com.android.sickfuture.sickcore.app.SickApp;
import com.android.sickfuture.sickcore.context.ContextHolder;
import com.android.sickfuture.sickcore.image.callback.ImageLoadedCallback;
import com.android.sickfuture.sickcore.image.drawable.RecyclingBitmapDrawable;
import com.android.sickfuture.sickcore.task.AsyncTask;
import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;
import com.android.sickfuture.sickcore.utils.Converter;
import com.android.sickfuture.sickcore.utils.L;
import com.android.sickfuture.sickcore.utils.NetworkHelper;

public class SickImageLoader implements IAppServiceKey {

	private static final String LOG_TAG = SickImageLoader.class.getSimpleName();
	public static final String SYSTEM_SERVICE_KEY = "framework:imageloader";

	private boolean mFadeIn = true;
	private static final int DEFAULT_FADE_IN_TIME = 600;
	private int mFadeInTime = DEFAULT_FADE_IN_TIME;

	private final static int DEFAULT_IMAGE_WIDTH = 300;
	private final static int DEFAULT_IMAGE_HEIGHT = 300;

	private Bitmap mPlaceHolderBitmap;

	private final Context mContext;
	private final Resources mResources;
	private final ImageCacher mImageCacher;
	private final ImageWorker mImageWorker;

	private final Object mPauseWorkLock = new Object();
	private boolean mPauseWork = false;

	private SickImageLoader(ImageLoaderParamsBuilder builder) {
		mContext = ContextHolder.getInstance().getContext();
		mResources = builder.mResources;
		mImageCacher = builder.mImageCacher;
		mImageWorker = builder.mImageWorker;
		mPlaceHolderBitmap = builder.mPlaceHolderImage;
		mFadeIn = builder.mFadeIn;
		mFadeInTime = builder.mFadeInTime;
	}

	public static class ImageLoaderParamsBuilder {

		private boolean DEFAULT_FADE_IN = true;
		private boolean mFadeIn = DEFAULT_FADE_IN;
		private static final int DEFAULT_FADE_IN_TIME = 600;
		private int mFadeInTime = DEFAULT_FADE_IN_TIME;

		private Bitmap mPlaceHolderImage;

		private final Resources mResources;
		private final ImageCacher mImageCacher;
		private final ImageWorker mImageWorker;

		public ImageLoaderParamsBuilder(Context context) {
			mResources = context.getResources();
			mImageCacher = new ImageCacher(context);
			mImageWorker = new ImageWorker(context, mImageCacher);
		}

		public ImageLoaderParamsBuilder setDiscCacheEnabled(boolean isEnabled) {
			mImageCacher.setDiscCacheEnabled(isEnabled);
			return this;
		}

		public ImageLoaderParamsBuilder setMemoryCacheEnabled(boolean isEnabled) {
			mImageCacher.setMemoryCacheEnabled(isEnabled);
			return this;
		}

		public ImageLoaderParamsBuilder setDiscCacheSize(
				int discCacheSizeInBytes) {
			mImageCacher.setDiscCacheSize(discCacheSizeInBytes);
			return this;
		}

		public ImageLoaderParamsBuilder setMemoryCacheSize(
				int memoryCacheSizeInBytes) {
			mImageCacher.setMemoryCacheSize(memoryCacheSizeInBytes);
			return this;
		}

		public ImageLoaderParamsBuilder setPartOfAvailibleMemoryCache(float part) {
			mImageCacher.setPartOfAvailableMemoryCache(part);
			return this;
		}

		public ImageLoaderParamsBuilder enableFadeIn(boolean isEnabled) {
			mFadeIn = isEnabled;
			return this;
		}

		public ImageLoaderParamsBuilder setFadeInTime(int time) {
			mFadeInTime = time;
			return this;
		}

		public ImageLoaderParamsBuilder setLoadingImage(int resDrawableID) {
			mPlaceHolderImage = BitmapFactory.decodeResource(mResources,
					resDrawableID);
			return this;
		}

		public ImageLoaderParamsBuilder setLoadingImage(Bitmap bitmap) {
			mPlaceHolderImage = bitmap;
			return this;
		}

		public SickImageLoader build() {
			return new SickImageLoader(this);
		}
	}

	@Override
	public String getKey() {
		return SickApp.IMAGE_LOADER_SERVICE;
	}

	public void setLoadingImage(int resDrawableID) {
		mPlaceHolderBitmap = BitmapFactory.decodeResource(mResources,
				resDrawableID);
	}

	public void setLoadingImage(Bitmap bitmap) {
		mPlaceHolderBitmap = bitmap;
	}

	public void setFadeIn(boolean fadeIn) {
		mFadeIn = fadeIn;
	}

	public void setFadeInTime(int time) {
		mFadeInTime = time;
	}

	public Bitmap loadBitmapSync(String url, int widthInPx, int heightInPx) {
		if (NetworkHelper.checkConnection(mContext)) {
			try {
				if (widthInPx > 0 && heightInPx > 0) {
					return mImageWorker.loadBitmap(mContext, url, widthInPx,
							heightInPx);
				} else {
					return mImageWorker.loadBitmap(mContext, url,
							DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
				}
			} catch (IOException e) {
				return mPlaceHolderBitmap;
			}
		} else {
			return mPlaceHolderBitmap;
		}

	}

	public Bitmap loadBitmapSync(String url) {
		return loadBitmapSync(url, 0, 0);
	}

	public Bitmap loadBitmap(String url, int widthInPx, int heightInPx) {
		BitmapDrawable bitmapDrawable = null;
		Bitmap result = null;
		if (mImageCacher != null) {
			bitmapDrawable = mImageCacher.getBitmapFromMemoryCache(url);
			if (bitmapDrawable != null) {
				result = bitmapDrawable.getBitmap();
			}
		}
		BitmapAsyncTask bitmapAsyncTask = null;
		if (result == null) {
			if (widthInPx > 0 && heightInPx > 0) {
				bitmapAsyncTask = new BitmapAsyncTask(widthInPx, heightInPx);
			} else {
				bitmapAsyncTask = new BitmapAsyncTask();
			}
			bitmapAsyncTask.start(url);
			try {
				result = bitmapAsyncTask.get();
			} catch (InterruptedException e) {
				L.e(LOG_TAG,
						"Can't load bitmap. Troubles with asynctask execution!");
			} catch (ExecutionException e) {
				L.e(LOG_TAG,
						"Can't load bitmap. Troubles with asynctask execution!");
			}
		} else {
			if (widthInPx > 0 && heightInPx > 0) {
				if (result.getWidth() != widthInPx
						|| result.getHeight() != heightInPx) {
					result.recycle();
					result = null;
					bitmapAsyncTask = new BitmapAsyncTask(widthInPx, heightInPx);
					try {
						result = bitmapAsyncTask.get();
					} catch (InterruptedException e) {
						L.e(LOG_TAG,
								"Can't load bitmap. Troubles with asynctask execution!");
					} catch (ExecutionException e) {
						L.e(LOG_TAG,
								"Can't load bitmap. Troubles with asynctask execution!");
					}
				}
			}
		}
		return result;
	}

	public Bitmap loadBitmap(Resources resources, String url, int widthInDp,
			int heightInDp) {
		return loadBitmap(url, (int) Converter.dpToPx(resources, widthInDp),
				(int) Converter.dpToPx(resources, heightInDp));
	}

	public Bitmap loadBitmap(String url) {
		return loadBitmap(url, 0, 0);
	}

	public void loadBitmap(ImageView imageView, String url, int widthInPx,
			int heightInPx) {
		if (TextUtils.isEmpty(url) || url == null) {
			L.e(LOG_TAG, "empty or null url");
			setImageDrawable(imageView, new BitmapDrawable(mResources,
					mPlaceHolderBitmap));
			return;
		}
		BitmapDrawable bitmapDrawable = mImageCacher
				.getBitmapFromMemoryCache(url);
		if (bitmapDrawable != null) {
			imageView.setImageDrawable(bitmapDrawable);
		} else if (cancelPotentialDownload(imageView, url)) {
			ImageAsyncTask bitmapAsyncTask = new ImageAsyncTask(imageView);
			AsyncBitmapDrawable asyncbitmapDrawable = new AsyncBitmapDrawable(
					mResources, mPlaceHolderBitmap, bitmapAsyncTask);
			imageView.setImageDrawable(asyncbitmapDrawable);
			if (widthInPx > 0 && heightInPx > 0) {
				bitmapAsyncTask.start(url, widthInPx, heightInPx);
			} else {
				bitmapAsyncTask.start(url);
			}
		}
	}

	private void loadBitmapWithCallback(ImageView imageView, String url,
			int widthInPx, int heightInPx, ImageLoadedCallback callback) {
		if (TextUtils.isEmpty(url) || url == null) {
			L.e(LOG_TAG, "empty or null url");
			setImageDrawable(imageView, new BitmapDrawable(mResources,
					mPlaceHolderBitmap));
			callback.onLoadError();
			return;
		}
		BitmapDrawable bitmapDrawable = mImageCacher
				.getBitmapFromMemoryCache(url);
		if (bitmapDrawable != null) {
			imageView.setImageDrawable(bitmapDrawable);
			callback.onLoadFinished();
		} else if (cancelPotentialDownload(imageView, url)) {
			ImageAsyncTask bitmapAsyncTask = new ImageAsyncTask(imageView,
					callback);
			AsyncBitmapDrawable asyncbitmapDrawable = new AsyncBitmapDrawable(
					mResources, mPlaceHolderBitmap, bitmapAsyncTask);
			imageView.setImageDrawable(asyncbitmapDrawable);
			if (widthInPx > 0 && heightInPx > 0) {
				bitmapAsyncTask.start(url, widthInPx, heightInPx);
			} else {
				bitmapAsyncTask.start(url);
			}
		}
	}

	public void loadBitmap(Resources resources, ImageView imageView,
			String url, int widthiInDp, int heightInDp) {
		loadBitmap(imageView, url,
				(int) Converter.dpToPx(resources, widthiInDp),
				(int) Converter.dpToPx(resources, heightInDp));
	}

	public void loadBitmap(ImageView imageView, String url) {
		loadBitmap(imageView, url, 0, 0);
	}

	public void loadBitmap(ImageView imageView, String url, int widthInPx,
			int heightInPx, ImageLoadedCallback callback) {
		callback.onLoadStarted();
		try {
			loadBitmapWithCallback(imageView, url, widthInPx, heightInPx,
					callback);
		} catch (Exception e) {
			callback.onLoadError();
			e.printStackTrace();
		}
	}

	public void loadBitmap(Resources resources, ImageView imageView,
			String url, int widthInDp, int heightInDp,
			ImageLoadedCallback callback) {
		loadBitmap(imageView, url,
				(int) Converter.dpToPx(resources, widthInDp),
				(int) Converter.dpToPx(resources, heightInDp), callback);
	}

	public void loadBitmap(ImageView imageView, String url,
			ImageLoadedCallback callback) {
		loadBitmap(imageView, url, 0, 0, callback);
	}

	public void combineMultipleImages(ImageView imageView, int widthInPx,
			int heightInPx, String... partsUrls) {
		if (partsUrls == null) {
			L.e(LOG_TAG, "empty or null urls");
			setImageDrawable(imageView, new BitmapDrawable(mResources,
					mPlaceHolderBitmap));
			return;
		}
		String cacheKey = null;
		for (int i = 0; i < partsUrls.length; i++) {
			cacheKey += partsUrls[i];
		}
		if (TextUtils.isEmpty(cacheKey) || cacheKey == null) {
			L.e(LOG_TAG, "empty or null url");
			setImageDrawable(imageView, new BitmapDrawable(mResources,
					mPlaceHolderBitmap));
			return;
		}

		BitmapDrawable bitmapDrawable = mImageCacher
				.getBitmapFromMemoryCache(cacheKey);
		if (bitmapDrawable != null) {
			imageView.setImageDrawable(bitmapDrawable);
		} else if ((cancelPotentialDownload(imageView, cacheKey))) {
			CombineBitmapTask bitmapTask = new CombineBitmapTask(imageView);
			AsyncBitmapDrawable asyncBitmapDrawable = new AsyncBitmapDrawable(
					mResources, mPlaceHolderBitmap, bitmapTask);
			imageView.setImageDrawable(asyncBitmapDrawable);
			bitmapTask.start(cacheKey, widthInPx, heightInPx, partsUrls);
		}
	}

	public void combineMultipleImages(Resources resources, ImageView imageView,
			int widthInPx, int heightInPx, String... partsUrls) {
		combineMultipleImages(imageView,
				(int) Converter.dpToPx(resources, widthInPx),
				(int) Converter.dpToPx(resources, heightInPx), partsUrls);
	}

	private static boolean cancelPotentialDownload(ImageView imageView,
			String url) {
		ImageAsyncTask bitmapAsyncTask = getImageLoaderTask(imageView);
		if (bitmapAsyncTask != null) {
			String bitmapUrl = bitmapAsyncTask.mUrl;
			if (bitmapUrl == null || !bitmapUrl.equals(url)) {
				bitmapAsyncTask.cancel(true);
				L.d(LOG_TAG, "cancelPotentialDownload for " + url);
			} else {
				return false;
			}
		}
		return true;
	}

	private static ImageAsyncTask getImageLoaderTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncBitmapDrawable) {
				final AsyncBitmapDrawable downloadedDrawable = (AsyncBitmapDrawable) drawable;
				return downloadedDrawable.getLoaderTask();
			}
		}
		return null;
	}

	private void setImageDrawable(ImageView imageView, Drawable drawable) {
		if (mFadeIn && drawable != null) {
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							drawable });
			if (AndroidVersionsUtils.hasJellyBean()) {
				setBackgroundPostJellyBean(imageView);
			} else {
				setBackgroundPreJellyBean(imageView);
			}
			imageView.setImageDrawable(td);
			td.startTransition(mFadeInTime);
		} else {
			imageView.setImageDrawable(drawable);
		}
	}

	@SuppressWarnings("deprecation")
	private void setBackgroundPreJellyBean(ImageView imageView) {
		imageView.setBackgroundDrawable(new BitmapDrawable(mResources,
				mPlaceHolderBitmap));
	}

	@SuppressLint("NewApi")
	private void setBackgroundPostJellyBean(ImageView imageView) {
		imageView.setBackground(new BitmapDrawable(mResources,
				mPlaceHolderBitmap));
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

	public class BitmapAsyncTask extends AsyncTask<String, Void, Bitmap> {

		private String mUrl;

		private int mWidth;
		private int mHeight;

		private boolean mDontCareAboutSize = false;

		public BitmapAsyncTask() {
			mWidth = DEFAULT_IMAGE_WIDTH;
			mHeight = DEFAULT_IMAGE_HEIGHT;
			mDontCareAboutSize = true;
		}

		public BitmapAsyncTask(int widthInPx, int heightInPx) {
			if (widthInPx > 0) {
				mWidth = widthInPx;
				mDontCareAboutSize = false;
			} else {
				mWidth = DEFAULT_IMAGE_WIDTH;
				mDontCareAboutSize = true;
			}
			if (heightInPx > 0) {
				mHeight = heightInPx;
				mDontCareAboutSize = false;
			} else {
				mHeight = DEFAULT_IMAGE_HEIGHT;
				mDontCareAboutSize = true;
			}
		}

		public void start(String url) {
			mUrl = url;
			// custom version of AsyncTask
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			if (mUrl == null || TextUtils.isEmpty(mUrl)) {
				L.e(LOG_TAG, "Can't load bitmap! Url empty or null!");
				return null;
			}
			Bitmap result = null;
			BitmapDrawable bitmapDrawable = null;
			if (mImageCacher != null) {
				bitmapDrawable = mImageCacher.getBitmapFromFileCache(mUrl);
			}
			if (bitmapDrawable != null) {
				result = bitmapDrawable.getBitmap();
			}
			if (result != null && mDontCareAboutSize) {
				return result;
			} else {
				try {
					if (result == null) {
						if (NetworkHelper.checkConnection(mContext)) {
							result = mImageWorker.loadBitmap(mUrl, mWidth,
									mHeight);
						}
					} else {
						if (!mDontCareAboutSize) {
							if (result.getWidth() != mWidth
									|| result.getHeight() != mHeight) {
								result.recycle();
								result = null;
								if (NetworkHelper.checkConnection(mContext)) {
									result = mImageWorker.loadBitmap(mUrl,
											mWidth, mHeight);
								}
							}
						}
						if (result != null) {
							if (AndroidVersionsUtils.hasHoneycomb()
									&& result.getConfig() != null) {
								bitmapDrawable = new BitmapDrawable(mResources,
										result);
							} else {
								bitmapDrawable = new RecyclingBitmapDrawable(
										mResources, result);
							}
							mImageCacher.put(mUrl, bitmapDrawable);
						}
					}
				} catch (IOException e) {
					return mPlaceHolderBitmap;
				}
				return result;
			}
		}
	}

	public class ImageAsyncTask extends AsyncTask<String, Void, BitmapDrawable> {

		protected String mUrl;

		private int mWidth = DEFAULT_IMAGE_WIDTH;
		private int mHeight = DEFAULT_IMAGE_HEIGHT;

		private WeakReference<ImageView> mImageViewReference;
		private ImageLoadedCallback mCallback;

		public ImageAsyncTask(ImageView imageView) {
			mImageViewReference = new WeakReference<ImageView>(imageView);
		}

		public ImageAsyncTask(ImageView imageView, ImageLoadedCallback callback) {
			mImageViewReference = new WeakReference<ImageView>(imageView);
			mCallback = callback;
		}

		public void start(String url) {
			mUrl = url;
			// custom version of AsyncTask
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		}

		public void start(String url, int width, int height) {
			mUrl = url;
			mWidth = width;
			mHeight = height;
			// custom version of AsyncTask
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		}

		@Override
		protected BitmapDrawable doInBackground(String... params) {
			if (TextUtils.isEmpty(mUrl)) {
				L.e(LOG_TAG, "Empty url!");
				return null;
			}
			synchronized (mPauseWorkLock) {
				while (mPauseWork && !isCancelled()) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
						// can be ignored
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
				if (NetworkHelper.checkConnection(mContext) && !isCancelled()
						&& getAttachedImageView() != null) {
					bitmap = mImageWorker.loadBitmap(mUrl, mWidth, mHeight);
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
			} catch (IOException e) {
				e.printStackTrace();
				return null;
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
				ImageAsyncTask bitmapDownloaderTask = getImageLoaderTask(imageView);
				// Change bitmap only if this process is still associated with
				// it
				if (this == bitmapDownloaderTask) {
					if (imageView != null && result != null) {
						setImageDrawable(imageView, result);
					}
				}
			}
			if (mCallback != null) {
				mCallback.onLoadFinished();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			synchronized (mPauseWorkLock) {
				mPauseWorkLock.notifyAll();
			}
		}

		protected ImageView getAttachedImageView() {
			final ImageView imageView = mImageViewReference.get();
			final ImageAsyncTask bitmapWorkerTask = getImageLoaderTask(imageView);

			if (this == bitmapWorkerTask) {
				return imageView;
			}
			return null;
		}
	}

	public class CombineBitmapTask extends ImageAsyncTask {

		private int DEFAULT_WIDTH = 300;
		private int DEFAULT_HEIGHT = 300;

		private int mWidth = DEFAULT_WIDTH;
		private int mHeight = DEFAULT_HEIGHT;

		private Map<String, float[]> positions;

		private Canvas mCanvas;
		private Bitmap mResultBitmap;

		public CombineBitmapTask(ImageView imageView) {
			super(imageView);
		}

		public void start(String cacheKey, int width, int height,
				String... partsUrls) {
			mUrl = cacheKey;
			if (width > 0) {
				mWidth = width;
			}
			if (height > 0) {
				mHeight = height;
			}
			executeOnExecutor(DUAL_THREAD_EXECUTOR, partsUrls);
		}

		@Override
		protected void onPreExecute() {
			mResultBitmap = Bitmap.createBitmap(mWidth, mHeight,
					Config.ARGB_8888);
			mCanvas = new Canvas(mResultBitmap);
		}

		@Override
		protected BitmapDrawable doInBackground(String... params) {
			positions = new HashMap<String, float[]>(params.length);
			countOffsets(0, 0, mWidth, mHeight, params);
			float[] offset = new float[4];
			for (int i = 0; i < params.length; i++) {
				synchronized (mPauseWorkLock) {
					while (mPauseWork && !isCancelled()) {
						try {
							mPauseWorkLock.wait();
						} catch (InterruptedException e) {
							// can be ignored
						}
					}
				}
				if (TextUtils.isEmpty(params[i])) {
					continue;
				}
				offset = positions.get(params[i]);
				try {
					// if bitmap not cached, so loading it
					if (NetworkHelper.checkConnection(mContext)
							&& !isCancelled() && getAttachedImageView() != null) {
						drawPart(mImageWorker.loadBitmap(params[i],
								(int) mWidth, (int) mHeight), offset);
					}
				} catch (IOException e) {
					L.e(LOG_TAG, "can't load part to combine images in bitmap");
					return null;
				} catch (Exception e) {
					L.e(LOG_TAG, "can't load part to combine images in bitmap");
					e.printStackTrace();
				}
			}
			BitmapDrawable bitmapDrawable = null;
			if (mResultBitmap != null) {
				if (AndroidVersionsUtils.hasHoneycomb()
						&& mResultBitmap.getConfig() != null) {
					bitmapDrawable = new BitmapDrawable(mResources,
							mResultBitmap);
				} else {
					bitmapDrawable = new RecyclingBitmapDrawable(mResources,
							mResultBitmap);
				}
				mImageCacher.putBitmapToMemoryCache(mUrl, bitmapDrawable);
			}
			return bitmapDrawable;
		}

		private void drawPart(Bitmap part, float[] offset) throws Exception {
			if (part != null && !part.isRecycled() && offset != null) {
				part = Bitmap.createScaledBitmap(part,
						(int) Math.abs(offset[2] - offset[0]),
						(int) Math.abs(offset[3] - offset[1]), false);
				mCanvas.drawBitmap(part, offset[0], offset[1], null);
				part.recycle();
				part = null;
			}
		}

		private void countOffsets(float posX0, float posY0, float posX1,
				float posY1, String[] urls) {
			float width = Math.abs(posX1 - posX0);
			float height = Math.abs(posY1 - posY0);
			if (urls.length == 1) {
				positions.put(urls[0],
						new float[] { posX0, posY0, posX1, posY1 });
				return; // the point of recursion exit
			}
			String[] splitedUrls1 = new String[urls.length / 2];
			String[] splitedUrls2 = new String[urls.length
					- splitedUrls1.length];
			System.arraycopy(urls, 0, splitedUrls1, 0, splitedUrls1.length);
			System.arraycopy(urls, splitedUrls1.length, splitedUrls2, 0,
					splitedUrls2.length);
			if (width >= height) {
				countOffsets(posX0, posY0, posX0 + (width / 2), posY1,
						splitedUrls1);
				countOffsets(posX0 + (width / 2), posY0, posX1, posY1,
						splitedUrls2);
			} else {
				countOffsets(posX0, posY0, posX1, posY0 + (height / 2),
						splitedUrls1);
				countOffsets(posX0, posY0 + (height / 2), posX1, posY1,
						splitedUrls2);
			}
		}
	}
}
