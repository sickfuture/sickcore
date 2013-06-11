package com.android.sickfuture.sickcore.image;

import android.graphics.Bitmap;

public abstract class Callback {

	abstract String getUrl();
	
	abstract boolean isCached();
	

	abstract void onSuccess(Bitmap bm);

	abstract void onError(Exception e);

	@Override
	public boolean equals(Object object){
		if(object instanceof Callback){
			Callback callback = (Callback) object;
			if (this.getUrl() != null && callback.getUrl() != null){
				if (this.getUrl().equals(callback.getUrl())){
					return true;
				}
			}
		}
		return false;
	}
}
