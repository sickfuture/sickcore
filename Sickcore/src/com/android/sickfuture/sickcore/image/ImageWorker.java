package com.android.sickfuture.sickcore.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.http.client.methods.HttpGet;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.android.sickfuture.sickcore.BuildConfig;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.http.HttpManager;
import com.android.sickfuture.sickcore.image.cache.ImageCacher;
import com.android.sickfuture.sickcore.utils.Calculate;
import com.android.sickfuture.sickcore.utils.L;

public class ImageWorker {

	private static final String LOG_TAG = ImageWorker.class.getSimpleName();

	private final HttpManager mHttpManager;
	private final ImageCacher mImageCacher;

	private static volatile ImageWorker instance;

	private ImageWorker(HttpManager httpManager, ImageCacher imageCacher) {
		mHttpManager = httpManager;
		mImageCacher = imageCacher;
	}

	public static ImageWorker getInstance(HttpManager httpManager,
			ImageCacher imageCacher) {
		ImageWorker localInstance = instance;
		if (localInstance == null) {
			synchronized (ImageWorker.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new ImageWorker(httpManager,
							imageCacher);
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
			BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length,
					options);
			int sampleSize = Calculate.calculateInSampleSize(options, reqWidth,
					reqHeight);
			if (BuildConfig.DEBUG) {
				L.d(LOG_TAG, "input width = " + options.outWidth + ", "
						+ "input height = " + options.outHeight);
				L.d(LOG_TAG, "sample size = " + sampleSize);
				L.d(LOG_TAG, "format = " + options.outMimeType);
			}
			options.inSampleSize = sampleSize;
			// options.inPreferredConfig = Config.RGB_565;
			options.inJustDecodeBounds = false;
			// if (AndroidVersionsUtils.hasHoneycomb()) {
			// addInBitmapOptions(options, mImageCacher);
			// }
			result = BitmapFactory.decodeByteArray(byteArray, 0,
					byteArray.length, options);
			if (BuildConfig.DEBUG) {
				if (result != null) {
					int height = result.getHeight();
					int width = result.getWidth();
					L.d(LOG_TAG, "output width = " + width + ", "
							+ "output height = " + height);
				}
			}
			return result;
		} finally {
			try {
				if (openStream != null) {
					openStream.close();
				}
			} catch (IOException e) {
				L.e(LOG_TAG, "Cant load bitmap from " + url);

			}
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static void addInBitmapOptions(BitmapFactory.Options options,
			ImageCacher cache) {
		options.inMutable = true;

		if (cache != null) {
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
