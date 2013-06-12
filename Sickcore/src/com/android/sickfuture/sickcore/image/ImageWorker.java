package com.android.sickfuture.sickcore.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.http.client.methods.HttpGet;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.android.sickfuture.sickcore.BuildConfig;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.http.HttpManager;
import com.android.sickfuture.sickcore.image.cache.ImageCacher;
import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;
import com.android.sickfuture.sickcore.utils.Calculate;
import com.android.sickfuture.sickcore.utils.L;

public class ImageWorker {

	private static final String LOG_TAG = ImageWorker.class.getSimpleName();

	private final HttpManager mHttpManager;
	private final ImageCacher mImageCacher;

	private static volatile ImageWorker instance;

	private ImageWorker(Context context) {
		mHttpManager = HttpManager.getInstance(context);
		mImageCacher = ImageCacher.getInstance(context);
	}

	public static ImageWorker getInstance(Context context) {
		ImageWorker localInstance = instance;
		if (localInstance == null) {
			synchronized (ImageWorker.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new ImageWorker(context);
				}
			}
		}
		return localInstance;
	}

	public Bitmap loadBitmap(String url, int reqWidth, int reqHeight)
			throws MalformedURLException, IOException, BadRequestException {
		InputStream openStream = null;
		byte[] byteArray = null;
		Bitmap result = null;
		try {
			openStream = mHttpManager.loadInputStream(new HttpGet(url));
			int streamLength = openStream.available();
			byteArray = new byte[streamLength];
			openStream.read(byteArray);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(byteArray, 0, streamLength, options);
			int sampleSize = Calculate.calculateInSampleSize(options, reqWidth,
					reqHeight);
			if (BuildConfig.DEBUG) {
				L.d(LOG_TAG, "input width = " + options.outWidth + ", "
						+ "input height = " + options.outHeight);
				L.d(LOG_TAG, "sample size = " + sampleSize);
			}
			options.inSampleSize = sampleSize;
			options.inPreferredConfig = Config.RGB_565;
			options.inJustDecodeBounds = false;
			if (AndroidVersionsUtils.hasHoneycomb()) {
				addInBitmapOptions(options, mImageCacher);
			}
			result = BitmapFactory.decodeByteArray(byteArray, 0, streamLength,
					options);
			if (result != null) {
				if (BuildConfig.DEBUG) {
					int height = result.getHeight();
					int width = result.getWidth();
					L.d(LOG_TAG, "output width = " + width + ", "
							+ "output height = " + height);
				}
			}
			return result;
		} finally {
			if (openStream != null) {
				openStream.close();
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static void addInBitmapOptions(BitmapFactory.Options options,
			ImageCacher cache) {
		// inBitmap only works with mutable bitmaps so force the decoder to
		// return mutable bitmaps.
		options.inMutable = true;

		if (cache != null) {
			// Try and find a bitmap to use for inBitmap
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);
			if (inBitmap != null) {
				if (BuildConfig.DEBUG) {
					L.d(LOG_TAG, "Found bitmap to use for inBitmap");
				}
				options.inBitmap = inBitmap;
			}
		}
	}
}
