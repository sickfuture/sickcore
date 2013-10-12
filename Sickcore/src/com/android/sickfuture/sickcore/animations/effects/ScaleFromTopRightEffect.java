package com.android.sickfuture.sickcore.animations.effects;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import com.android.sickfuture.sickcore.animations.effects.base.BaseEffect;

public class ScaleFromTopRightEffect extends BaseEffect {

	@Override
	public Animation getEffect(View view, int position, int scrollDirection) {
		mAnimation = new ScaleAnimation(0f, 1.0f, 0f, 1.0f, view.getWidth(), 0);
		mAnimation.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
		return mAnimation;
	}

}
