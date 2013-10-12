package com.android.sickfuture.sickcore.animations.effects.base;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;

public abstract class BaseEffect {

	public static final AccelerateDecelerateInterpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();

	protected Animation mAnimation;

	public abstract Animation getEffect(View view, int position,
			int scrollDirection);

}
