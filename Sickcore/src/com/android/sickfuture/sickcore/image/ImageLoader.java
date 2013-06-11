package com.android.sickfuture.sickcore.image;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.android.sickfuture.sickcore.asynctask.CustomExecutorAsyncTask;
import com.android.sickfuture.sickcore.collections.SetArrayList;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.http.HttpManager;
import com.android.sickfuture.sickcore.image.cache.ImageCacher;
import com.android.sickfuture.sickcore.utils.L;

public class ImageLoader {

	private static final String LOG_TAG = "ImageLoader";

	private static final int FADE_IN_TIME = 600;

	private static ImageLoader instance;

	private final Resources mResources;

	private final ImageCacher mImageCacher;

	private final HttpManager mHttpManager;

	private Drawable mLoadingImage;

	private final Object mPauseWorkLock = new Object();
	private boolean mPauseWork = false;

	private final Object mIsLoadingNewLock = new Object();
	private boolean mIsLoadingNew = true;

	private List<Callback> mQueue;

	private Map<ImageView, Callback> mViewsCallbacks;

	protected int mNumberOnExecute;

	private boolean mFadeInBitmap = true;

	public static ImageLoader getInstance(Context context) {
		ImageLoader localInstance = instance;
		if (instance == null) {
			synchronized (ImageLoader.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new ImageLoader(context);
				}
			}
		}
		return localInstance;
	}

	private ImageLoader(Context context) {
		mQueue = Collections.synchronizedList(new SetArrayList<Callback>());
		mViewsCallbacks = Collections
				.synchronizedMap(new HashMap<ImageView, Callback>());
		mImageCacher = ImageCacher.getInstance();
		mResources = context.getResources();
		mHttpManager = HttpManager.getInstance(context);
	}

	public void bind(final BaseAdapter adapter, final ImageView imageView,
			final String url, final boolean cacheOnDiskMemory) {
		imageView.setImageDrawable(mLoadingImage);
		L.d("ImageQueue", String.valueOf(mQueue.size()));
		Bitmap bitm = null;
		bitm = mImageCacher.getBitmapFromMemoryCache(url);
		if (bitm != null) {
			imageView.setImageBitmap(bitm);
			// setImageDrawable(imageView, bitm);
		} else {

			mQueue.add(0, new Callback() {

				public boolean isCached() {
					return cacheOnDiskMemory;
				}

				public void onSuccess(Bitmap bm) {
					adapter.notifyDataSetChanged();
				}

				public void onError(Exception e) {
					// TODO set error image
				}

				public String getUrl() {
					return url;
				}
			});
		}
		proceed();
	}

	@SuppressWarnings("deprecation")
	public void bindOld(final ImageView imageView, final String url,
			final boolean cacheOnDiskMemory) {
		imageView.setBackgroundDrawable(mLoadingImage);
		imageView.setImageBitmap(null);
		Bitmap bitm = null;
		bitm = mImageCacher.getBitmapFromMemoryCache(url);
		if (bitm != null) {
			setImageDrawable(imageView, new BitmapDrawable(mResources, bitm));
		} else {
			imageView.setTag(url);
			mQueue.add(0, new Callback() {

				public boolean isCached() {
					return cacheOnDiskMemory;
				}

				public void onSuccess(Bitmap bm) {
					if (imageView.getTag().equals(url)) {
						setImageDrawable(imageView, new BitmapDrawable(
								mResources, bm));
					}
				}

				public void onError(Exception e) {
					L.d(LOG_TAG, "ONERROR");
					if (imageView.getTag().equals(url)) {
						mQueue.add(0, this);
					}
				}

				public String getUrl() {
					return url;
				}
			});

		}
		proceed();
	}

	public void bind(final ImageView imageView, final String url,
			final boolean cacheOnDiskMemory) {
		imageView.setBackgroundDrawable(mLoadingImage);
		imageView.setImageBitmap(null);
		imageView.setTag(url);
		Bitmap bitm = null;
		bitm = mImageCacher.getBitmapFromMemoryCache(url);
		if (bitm != null) {
			setImageDrawable(imageView, new BitmapDrawable(mResources, bitm));
			mViewsCallbacks.remove(imageView);
		} else {
			mViewsCallbacks.put(imageView, new Callback() {

				public boolean isCached() {
					return cacheOnDiskMemory;
				}

				public void onSuccess(Bitmap bm) {
					if (!mViewsCallbacks.containsKey(imageView) && imageView.getTag().equals(url)){
						setImageDrawable(imageView, new BitmapDrawable(
								mResources, bm));
					}
				}

				public void onError(Exception e) {
					L.d(LOG_TAG, "ONERROR");
				}

				public String getUrl() {
					return url;
				}
			});

		}
		proceed();
	}

	public void setLoadingImage(Drawable loadingDrawable) {
		mLoadingImage = loadingDrawable;
	}

	public void setLoadingImage(int resDrawableID) {
		mLoadingImage = mResources.getDrawable(resDrawableID);
	}

	public void setLoadingImage(Bitmap bitmap) {
		mLoadingImage = new BitmapDrawable(mResources, bitmap);
	}

	private void proceed() {
		//L.d(LOG_TAG, "mVC size = " + String.valueOf(mViewsCallbacks.size()) + "   " + String.valueOf(mNumberOnExecute));
		if (mViewsCallbacks.isEmpty()) {
			return;
		}
		if (mNumberOnExecute < 5 && mIsLoadingNew) {
			Iterator<ImageView> iterator = mViewsCallbacks.keySet().iterator();
			while (mNumberOnExecute < 5 && !mViewsCallbacks.isEmpty()
					&& iterator.hasNext()) {
				final Callback callback = mViewsCallbacks.get(iterator
						.next());
				iterator.remove();
				new ImageLoaderTask(callback).start();
			}
		}

		// if (mNumberOnExecute > 5) {
		// if (mQueue.size() > 2)
		// mQueue.remove(mQueue.size() - 1);
		// return;
		// }
		// if (mQueue.isEmpty()) {
		// return;
		// }
		// final Callback callback = mQueue.remove(0);
		// new ImageLoaderTask(callback, cacheOnDiskMemory).start();
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
			mQueue.clear();
			mPauseWork = pauseWork;
			if (!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

	public void setIsLoadingNew(boolean loadingNew) {
		synchronized (mIsLoadingNewLock) {
			mIsLoadingNew = loadingNew;
			if (loadingNew){
				proceed();
			}
		}
	}

	private void setImageDrawable(final ImageView imageView,
			final Drawable drawable) {
		if (mFadeInBitmap) {
			// Transition drawable with a transparent drawable and the final
			// drawable
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							drawable });
			// Set background to loading bitmap
			imageView.setImageDrawable(td);
			td.startTransition(FADE_IN_TIME);
		} else {
			imageView.setImageDrawable(drawable);
		}
	}

	public class ImageLoaderTask extends
			CustomExecutorAsyncTask<Callback, Void, Object> {

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

		private Callback mCallback;

		private final Object mPauseWorkLock = new Object();
		private boolean mPauseWork = false;

		public ImageLoaderTask(Callback callback) {
			mCallback = callback;
		}

		public void start() {
			execute(mExecutor);
		}

		@Override
		protected void onPreExecute() {
			mNumberOnExecute++;
		}

		@Override
		protected Object doInBackground(Callback... params) {
			String url = mCallback.getUrl();
			if (TextUtils.isEmpty(url)) {
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
				bitmap = mImageCacher.getBitmapFromFileCache(url);
				if (bitmap != null) {
					mImageCacher.putBitmapToMemoryCache(url, bitmap);
				}
				if (bitmap != null) {
					return bitmap;
				}
				try {
					if (mHttpManager.isAvalibleInetConnection()) {
						bitmap = mHttpManager.loadBitmap(url, 500, 500);
					}
					if (bitmap != null) {
						mImageCacher.putBitmapToCache(url, bitmap,
								mCallback.isCached());
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
			if (result instanceof Bitmap) {
				mCallback.onSuccess((Bitmap) result);
			}
			if (result instanceof Exception) {
				mCallback.onError((Exception) result);
			}
			Iterator<ImageView> iterator = mViewsCallbacks.keySet().iterator();
			while (iterator.hasNext()) {
				ImageView iv = iterator.next();
				Callback callback = mViewsCallbacks.get(iv);
				if (this.equals(callback)) {
					iterator.remove();
					if (result instanceof Bitmap) {
						callback.onSuccess((Bitmap) result);
					}
					if (result instanceof Exception) {
						callback.onError((Exception) result);
					}
				}
			}
			mNumberOnExecute--;
			proceed();
		}
	}

}