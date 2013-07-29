package com.android.sickfuture.sickcore.adapter;

import java.util.Stack;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;

public abstract class AnimatedBaseCursorAdapter extends BaseCursorAdapter {

	private static final String LOG_TAG = AnimatedBaseCursorAdapter.class
			.getSimpleName();
	protected boolean mAnimationLock = false;
	protected Stack<View> mAnimatedViews;

	public AnimatedBaseCursorAdapter(Context context, Cursor c) {
		super(context, c);
		mAnimatedViews = new Stack<View>();
	}

	@Override
	public void notifyDataSetChanged() {
		setAnimated(false);
		super.notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		if (mAnimationLock) {
			setScrollAnimation(v, position);
		}
		return v;
	}

	public void setAnimated(boolean isAnimated) {
		this.mAnimationLock = isAnimated;
	}

	/**
	 * Canceling animations in each views
	 * */
	public void cancelAnimations() {
		View currentView = null;
		while (!mAnimatedViews.empty()) {
			synchronized (mAnimatedViews) {
				currentView = mAnimatedViews.pop();
			}
			if (currentView != null) {
				currentView.clearAnimation();
			}
		}
	}

	protected void setScrollAnimation(final View view, int position) {
		Animation animation = new ScaleAnimation(1.0f, 1.0f, 0f, 1.0f);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				synchronized (mAnimatedViews) {
					mAnimatedViews.add(view);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				synchronized (mAnimatedViews) {
					mAnimatedViews.remove(view);
				}
			}
		});
		animation.setDuration(300);
		view.setAnimation(animation);
	}

}
