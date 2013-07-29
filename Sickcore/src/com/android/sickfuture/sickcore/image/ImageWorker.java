package com.android.sickfuture.sickcore.image;


import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.android.sickfuture.sickcore.app.SickApp;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.source.implemented.HttpInputStreamDataSource;
import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;
import com.android.sickfuture.sickcore.utils.AppUtils;
import com.android.sickfuture.sickcore.utils.Calculate;
import com.android.sickfuture.sickcore.utils.IOUtils;
import com.android.sickfuture.sickcore.utils.L;

public class ImageWorker {

	private static final String GIF = "image/gif";

	private static final String LOG_TAG = ImageWorker.class.getSimpleName();

	public static final String SYSTEM_SERVICE_KEY = "sickcore:imageworker";

	// private static final int IO_BUFFER_SIZE = 8 * 1024;

	private final ImageCacher mImageCacher;

	private HttpInputStreamDataSource mDataSource;

	protected ImageWorker(Context context, ImageCacher imageCacher) {
		mDataSource = (HttpInputStreamDataSource) AppUtils.get(context,
				SickApp.HTTP_INPUT_STREAM_SERVICE_KEY);
		mImageCacher = imageCacher;
	}

	public Bitmap loadBitmap(String url, int reqWidth, int reqHeight)
			throws IOException {
		InputStream is = null;
		FlushedInputStream fis = null;
		try {
			try {
				is = mDataSource.getSource(url);
			} catch (BadRequestException e) {
				// can be ignore
			}
			if (is == null) {
				return null;
			}
			fis = new FlushedInputStream(is);
			fis.mark(is.available());
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(fis, null, options);
			int sampleSize = Calculate.calculateInSampleSize(options, reqWidth,
					reqHeight);
			L.d(LOG_TAG, "input width = " + options.outWidth + ", "
					+ "input height = " + options.outHeight);
			L.d(LOG_TAG, "sample size = " + sampleSize);
			L.d(LOG_TAG, "format = " + options.outMimeType);
			options.inSampleSize = sampleSize;
			options.inJustDecodeBounds = false;
			if (AndroidVersionsUtils.hasHoneycomb()) {
				if (!options.outMimeType.equals(GIF)) {
					addInBitmapOptions(options, mImageCacher);
				}
			}
			fis.reset();
			Bitmap result = BitmapFactory.decodeStream(fis, null, options);
			if (result != null) {
				L.d(LOG_TAG, "output width = " + result.getWidth() + ", "
						+ "output height = " + result.getHeight());
				L.d(LOG_TAG, "result config" + result.getConfig());
			}
			return result;
		} finally {
			IOUtils.closeStream(is);
			IOUtils.closeStream(fis);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static void addInBitmapOptions(BitmapFactory.Options options,
			ImageCacher cache) {
		options.inMutable = true;

		if (cache != null && options != null) {
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);
			if (inBitmap != null) {
				L.d(LOG_TAG, "Found bitmap to use for inBitmap");
				options.inBitmap = inBitmap;
			}
		}
	}
}
