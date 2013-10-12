package com.android.sickfuture.sickcore.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import com.android.sickfuture.sickcore.animations.ListViewAnimationHelper;
import com.android.sickfuture.sickcore.animations.ListViewAnimationHelper.Effects;
import com.android.sickfuture.sickcore.animations.effects.base.BaseEffect;

public class AnimatedListView extends ListView {

	private ListViewAnimationHelper mAnimationHelper;

	public AnimatedListView(Context context) {
		super(context);
		init();
	}

	public AnimatedListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public AnimatedListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		mAnimationHelper = new ListViewAnimationHelper();
		super.setOnScrollListener(mAnimationHelper);
	}

	public void replaceChildToFirstPos(long childId) {
		mAnimationHelper.replaceChildToFirstPos(this, childId);
	}

	public void animateAddition() {
		mAnimationHelper.animateAddition(this);
	}

	@Override
	public final void setOnScrollListener(OnScrollListener l) {
		mAnimationHelper.setOnScrollListener(l);
	}

	public void setMaxScrollSpeed(int maxScrollSpeed) {
		mAnimationHelper.setMaxScrollSpeed(maxScrollSpeed);
	}

	public void setDuration(int duration) {
		mAnimationHelper.setDuration(duration);
	}

	public void setAnimateOnlyOnFling(boolean animateOnlyOnFling) {
		mAnimationHelper.setAnimateOnlyOnFling(animateOnlyOnFling);
	}

	public void setScrollAnimationEffect(BaseEffect effect) {
		mAnimationHelper.setScrollAnimationEffect(effect);
	}

	public void setScrollAnimationEffect(Effects effects) {
		mAnimationHelper.setScrollAnimationEffect(effects);
	}

	public void setAdditionAnimationEffect(BaseEffect effect) {
		mAnimationHelper.setAdditionAnimationEffect(effect);
	}

	public void setAdditionAnimationEffect(Effects effects) {
		mAnimationHelper.setAdditionAnimationEffect(effects);
	}

}
