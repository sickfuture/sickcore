package com.android.sickfuture.sickcore.image.cache.disc;

import java.io.File;

import android.graphics.Bitmap;

public interface IDiscCache {
	
	public Bitmap get(String name);
	
	public File put(String name, Bitmap value);
	
	public boolean clear();

}
