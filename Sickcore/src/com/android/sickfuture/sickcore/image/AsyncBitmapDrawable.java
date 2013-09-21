package com.android.sickfuture.sickcore.image;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.android.sickfuture.sickcore.image.SickImageLoader.ImageAsyncTask;

public class AsyncBitmapDrawable extends BitmapDrawable {

	private WeakReference<ImageAsyncTask> mDrawableTaskReference;

	public AsyncBitmapDrawable(Resources resources, Bitmap loadingBitmap,
			ImageAsyncTask loaderTask) {
		super(resources, loadingBitmap);
		mDrawableTaskReference = new WeakReference<ImageAsyncTask>(loaderTask);
	}

	public ImageAsyncTask getLoaderTask() {
		return mDrawableTaskReference.get();
	}

}
