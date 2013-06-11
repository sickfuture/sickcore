package com.android.sickfuture.sickcore.utils;

import android.net.Uri;

import com.android.sickfuture.sickcore.annotations.ContentInfo;

public class ContractUtils {

	private ContractUtils() {
	}

	public static Uri getProviderUriFromContract(Class<?> contract) {
		ContentInfo contentInfo = contract.getAnnotation(ContentInfo.class);
		return Uri.parse(contentInfo.contentUri());
	}

	public static String getConentTypeFromContract(Class<?> contract) {
		ContentInfo contentInfo = contract.getAnnotation(ContentInfo.class);
		return contentInfo.contentType();
	}

}
