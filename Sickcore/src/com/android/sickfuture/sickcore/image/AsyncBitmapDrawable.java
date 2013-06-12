package com.android.sickfuture.sickcore.image;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.android.sickfuture.sickcore.image.SuperImageLoader.BitmapAsyncTask;

public class AsyncBitmapDrawable extends BitmapDrawable {

	private WeakReference<BitmapAsyncTask> mDrawableTaskReference;

	public AsyncBitmapDrawable(Resources resources, Bitmap loadingBitmap,
			BitmapAsyncTask loaderTask) {
		super(resources, loadingBitmap);
		mDrawableTaskReference = new WeakReference<BitmapAsyncTask>(loaderTask);
	}

	public BitmapAsyncTask getLoaderTask() {
		return mDrawableTaskReference.get();
	}

}
