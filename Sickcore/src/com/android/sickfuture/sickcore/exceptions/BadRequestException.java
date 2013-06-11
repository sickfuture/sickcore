package com.android.sickfuture.sickcore.exceptions;

public class BadRequestException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8744431949528341990L;

	public BadRequestException() {
		super();
	}

	public BadRequestException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public BadRequestException(String detailMessage) {
		super(detailMessage);
	}

	public BadRequestException(Throwable throwable) {
		super(throwable);
	}

}
