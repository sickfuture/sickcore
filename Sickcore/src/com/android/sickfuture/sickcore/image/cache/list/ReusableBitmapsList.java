package com.android.sickfuture.sickcore.image.cache.list;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import android.graphics.Bitmap;

public class ReusableBitmapsList extends ArrayList<SoftReference<Bitmap>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Object mPauseWorkLock = new Object();
	private boolean mPauseWork = false;

	@Override
	public boolean add(SoftReference<Bitmap> bitmap) {
		// syncModifying();
		// try {
		// setPauseWork(true);
		if (!this.contains(bitmap)) {
			return super.add(bitmap);
		}
		return false;
		// } finally {
		// setPauseWork(false);
		// }
	}

	// @Override
	// public SoftReference<Bitmap> get(int index) {
	// syncModifying();
	// try {
	// setPauseWork(true);
	// return super.get(index);
	// } finally {
	// setPauseWork(false);
	// }
	// }

	@Override
	public SoftReference<Bitmap> remove(int index) {
		syncModifying();
		try {
			setPauseWork(true);
			return super.remove(index);
		} finally {
			setPauseWork(false);
		}
	}

	@Override
	public boolean remove(Object object) {
		syncModifying();
		try {
			setPauseWork(true);
			if (this.contains(object)) {
				return super.remove(object);
			}
		} finally {
			setPauseWork(false);
		}
		return false;
	}

	private void syncModifying() {
		synchronized (mPauseWorkLock) {
			while (mPauseWork) {
				try {
					mPauseWorkLock.wait();
				} catch (InterruptedException e) {
					// TODO smth
				}
			}
		}
	}

	public void setPauseWork(boolean pauseWork) {
		synchronized (mPauseWorkLock) {
			mPauseWork = pauseWork;
			if (!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

}
