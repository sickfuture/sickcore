package com.android.sickfuture.sickcore.animations;

import java.util.HashMap;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.android.sickfuture.sickcore.animations.effects.ScaleFromLeftEffect;
import com.android.sickfuture.sickcore.animations.effects.ScaleFromTopEffect;
import com.android.sickfuture.sickcore.animations.effects.ScaleFromTopLeftEffect;
import com.android.sickfuture.sickcore.animations.effects.ScaleFromTopRightEffect;
import com.android.sickfuture.sickcore.animations.effects.base.BaseEffect;
import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;

public class ListViewAnimationHelper implements OnScrollListener {

	private static final int DEFAULT_DURATION = 1000;
	private final static int DEFAULT_MAX_SPEED = 13;

	private int mDuration = DEFAULT_DURATION;
	private int mMaxScrollSpeed = DEFAULT_MAX_SPEED;

	private int mFirstVisibleItem = -1;
	private int mLastVisibleItem = -1;

	private OnScrollListener mOnScrollListener;

	private boolean mIsScrolling;
	private boolean mIsFling;

	private boolean mAnimateOnlyOnFling;

	private BaseEffect mScrollingEffect;
	private BaseEffect mAdditionEffect;

	private HashMap<Long, Integer> mItemIdTopMap;

	// TODO add new effects
	public static enum Effects {
		NO_EFFECTS, SCALE_FROM_TOP_EFFECT, SCALE_FROM_LEFT_EFFECT, SCALE_FROM_TOP_LEFT_EFFECT, SCALE_FROM_TOP_RIGHT_EFFECT
	}

	public ListViewAnimationHelper() {
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		try {
			if (mScrollingEffect == null) {
				return;
			}
			int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
			if (mIsScrolling && mFirstVisibleItem != -1
					&& mLastVisibleItem != -1) {
				controllScrollSpeed(firstVisibleItem, totalItemCount);
				int indexAfterFirst = 0;
				while (firstVisibleItem + indexAfterFirst < mFirstVisibleItem) {
					View item = view.getChildAt(indexAfterFirst);
					animateScrolling(view, item, firstVisibleItem
							+ indexAfterFirst, -1, firstVisibleItem,
							visibleItemCount);
					indexAfterFirst++;
				}

				int indexBeforeLast = 0;
				while (lastVisibleItem - indexBeforeLast > mLastVisibleItem) {
					View item = view.getChildAt(lastVisibleItem
							- firstVisibleItem - indexBeforeLast);
					animateScrolling(view, item, lastVisibleItem
							- indexBeforeLast, 1, firstVisibleItem,
							visibleItemCount);
					indexBeforeLast++;
				}
			}

			mFirstVisibleItem = firstVisibleItem;
			mLastVisibleItem = lastVisibleItem;
		} finally {
			if (mOnScrollListener != null) {
				mOnScrollListener.onScroll(view, firstVisibleItem,
						visibleItemCount, totalItemCount);
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		try {
			if (mScrollingEffect == null) {
				return;
			}
			switch (scrollState) {
			case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
				mIsScrolling = false;
				mIsFling = false;
				break;
			case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
				mIsFling = true;
				break;
			case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				mIsScrolling = true;
				mIsFling = false;
				break;
			default:
				break;
			}
		} finally {
			if (mOnScrollListener != null) {
				mOnScrollListener.onScrollStateChanged(view, scrollState);
			}
		}
	}

	public void setOnScrollListener(OnScrollListener listener) {
		mOnScrollListener = listener;
	}

	private void animateScrolling(AbsListView view, View item, int position,
			int scrollDirection, int firstVisibleItem, int visibleItemCount) {
		if (item == null) {
			return;
		}

		if (mAnimateOnlyOnFling && !mIsFling) {
			return;
		}

		// header and footed are not animated
		if (view.getAdapter() != null) {
			if (view.getAdapter().getItemViewType(position) == ListView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
				return;
			}
		}

		if (mSpeed > mMaxScrollSpeed) {
			cancelAnimations(view, position, scrollDirection, firstVisibleItem,
					visibleItemCount);
			return;
		}
		Animation animation = mScrollingEffect.getEffect(item, position,
				scrollDirection);
		if (animation != null) {
			animation.setDuration(mDuration);
			item.setAnimation(animation);
		}
	}

	private int mPreviousFirstVisibleItem = 0;
	private long mPreviousEventTime = 0;
	private double mSpeed = 0;

	// twotoasters issue
	public void controllScrollSpeed(int firstVisibleItem, int scrollState) {
		if (mMaxScrollSpeed > 0
				&& mPreviousFirstVisibleItem != firstVisibleItem) {
			long currTime = System.currentTimeMillis();
			long timeToScrollOneItem = currTime - mPreviousEventTime;
			if (timeToScrollOneItem < 1) {
				double newSpeed = ((1.0d / timeToScrollOneItem) * 1000);
				if (newSpeed < (0.9f * mSpeed)) {
					mSpeed *= 0.9f;
				} else if (newSpeed > (1.1f * mSpeed)) {
					mSpeed *= 1.1f;
				} else {
					mSpeed = newSpeed;
				}
			} else {
				mSpeed = ((1.0d / timeToScrollOneItem) * 1000);
			}

			mPreviousFirstVisibleItem = firstVisibleItem;
			mPreviousEventTime = currTime;
		}
	}

	/**
	 * Canceling animations when over scroll speed limit
	 * */
	protected void cancelAnimations(AbsListView view, int position,
			int scrollDirection, int firstVisibleItem, int visibleItemCount) {
		int childPos = position - firstVisibleItem;
		if (scrollDirection > 0) {
			for (int i = childPos; i > 0; i--) {
				View item = view.getChildAt(i);
				if (item != null) {
					if (item.getAnimation() != null)
						item.clearAnimation();
				}
			}
		} else {
			for (int i = childPos; i < visibleItemCount; i++) {
				View item = view.getChildAt(i);
				if (item != null) {
					if (item.getAnimation() != null)
						item.clearAnimation();
				}
			}
		}
	}

	public void replaceChildToFirstPos(ListView view, long childId) {
		if (!AndroidVersionsUtils.hasHoneycombMR1()) {
			return;
		}
		ListAdapter adapter = view.getAdapter();
		if (adapter == null) {
			return;
		}
		if (!adapter.hasStableIds()) {
			throw new IllegalArgumentException(
					"Adapter shold has stable ids for replacing animations");
		}
		View childToReplace = null;
		int firstVisiblePos = view.getFirstVisiblePosition();
		for (int i = 0; i < view.getChildCount(); i++) {
			// finding view to animate
			long currentId = view.getItemIdAtPosition(firstVisiblePos + i);
			if (currentId == childId) {
				childToReplace = view.getChildAt(i);
				break;
			}
		}

		animateReplacing(view, adapter, childToReplace, childId);

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void animateReplacing(final ListView listView,
			final ListAdapter adapter, View childToReplace, final long childId) {

		if (childToReplace == null) {
			childToReplace = listView.getChildAt(listView.getChildCount() - 1);
		}

		if (childToReplace == null) {
			return;
		}

		mItemIdTopMap = new HashMap<Long, Integer>();

		final int fromTop = childToReplace.getTop();

		int firstVisiblePosition = listView.getFirstVisiblePosition();
		if (listView.getPositionForView(childToReplace) == 0) {
			return;
		}
		for (int i = 0; i < listView.getChildCount(); ++i) {
			View child = listView.getChildAt(i);
			if (child != childToReplace) {
				int position = firstVisiblePosition + i;
				long itemId = adapter.getItemId(position);
				mItemIdTopMap.put(itemId, child.getTop());
			}
		}

		final ViewTreeObserver observer = listView.getViewTreeObserver();
		observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			public boolean onPreDraw() {
				observer.removeOnPreDrawListener(this);
				boolean firstAnimation = true;
				int firstVisiblePosition = listView.getFirstVisiblePosition();
				for (int i = 0; i < listView.getChildCount(); ++i) {
					final View child = listView.getChildAt(i);
					int position = firstVisiblePosition + i;
					long itemId = adapter.getItemId(position);
					Integer startTop = mItemIdTopMap.get(itemId);
					int top = child.getTop();
					if (startTop != null) {
						// animate other views
						if (startTop != top) {
							// if view location changed
							int delta = startTop - top;
							child.setTranslationY(delta);
							child.animate().setDuration(mDuration)
									.translationY(0);
						}
					} else {

						if (fromTop == child.getTop()) {
							continue;
						}

						int childToReplacePos = findChildPos(adapter, childId);

						if (childToReplacePos == -1) {
							continue;
						}

						final View replacingView = adapter.getView(
								childToReplacePos, child, listView);
						int translationY = 0;
						int delta = fromTop - top;
						if (listView.getPositionForView(listView.getChildAt(0)) != 0) {
							int childHeight = replacingView.getHeight()
									+ listView.getDividerHeight();
							translationY -= childHeight;
						}
						child.setTranslationY(delta);
						child.animate()
								.setDuration(mDuration)
								.translationY(translationY)
								.setInterpolator(
										BaseEffect.ACCELERATE_DECELERATE_INTERPOLATOR);
						if (firstAnimation) {
							child.animate().setListener(new AnimatorListener() {

								@Override
								public void onAnimationStart(Animator animation) {
								}

								@Override
								public void onAnimationRepeat(Animator animation) {
								}

								@Override
								public void onAnimationEnd(Animator animation) {
									if (replacingView.getTranslationY() != 0) {
										replacingView.animate().translationY(0);
									}
									notifyListViewItems(listView, adapter);
								}

								@Override
								public void onAnimationCancel(Animator animation) {
								}
							});
							firstAnimation = false;
						}
					}
				}
				mItemIdTopMap.clear();
				return true;
			}

			private int findChildPos(ListAdapter adapter, long childId) {
				for (int i = 0; i < adapter.getCount(); i++) {
					if (adapter.getItemId(i) == childId) {
						return i;
					}
				}
				return -1;
			}
		});
	}

	public void animateAddition(ListView listView) {
		if (mAdditionEffect == null) {
			return;
		}
		ListAdapter adapter = listView.getAdapter();
		if (adapter == null) {
			return;
		}
		View convertView = null;
		int position = -1;
		if (listView.isStackFromBottom()) {
			convertView = listView.getChildAt(listView.getChildCount() - 1);
			position = listView.getCount() - 1;
			while (adapter.getItemViewType(position) == ListView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
				position--;
			}
		} else {
			convertView = listView.getChildAt(0);
			position = 0;
			while (adapter.getItemViewType(position) == ListView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
				position++;
			}
		}

		if (position < 0) {
			return;
		}
		View animatedView = adapter.getView(position, convertView, listView);
		if (animatedView != null
				&& animatedView.getVisibility() == View.VISIBLE) {
			Animation animation = mAdditionEffect.getEffect(animatedView,
					position, 0);
			if (animation != null) {
				animation.setDuration(mDuration);
				animatedView.setAnimation(animation);
			}
		}
	}

	public void setMaxScrollSpeed(int maxScrollSpeed) {
		if (maxScrollSpeed > 0) {
			this.mMaxScrollSpeed = maxScrollSpeed;
		} else {
			maxScrollSpeed = 0;
		}
	}

	public void setDuration(int duration) {
		if (duration > 0) {
			this.mDuration = duration;
		} else {
			duration = 0;
		}
	}

	public void setAnimateOnlyOnFling(boolean animateOnlyOnFling) {
		mAnimateOnlyOnFling = animateOnlyOnFling;
	}

	public void setScrollAnimationEffect(BaseEffect effect) {
		mScrollingEffect = effect;
	}

	public void setScrollAnimationEffect(Effects effects) {
		switch (effects) {
		case NO_EFFECTS:
			mScrollingEffect = null;
			break;
		case SCALE_FROM_TOP_EFFECT:
			mScrollingEffect = new ScaleFromTopEffect();
			break;
		case SCALE_FROM_LEFT_EFFECT:
			mScrollingEffect = new ScaleFromLeftEffect();
			break;
		case SCALE_FROM_TOP_LEFT_EFFECT:
			mScrollingEffect = new ScaleFromTopLeftEffect();
			break;
		case SCALE_FROM_TOP_RIGHT_EFFECT:
			mScrollingEffect = new ScaleFromTopRightEffect();
			break;
		default:
			break;
		}
	}

	public void setAdditionAnimationEffect(BaseEffect effect) {
		mAdditionEffect = effect;
	}

	public void setAdditionAnimationEffect(Effects effects) {
		switch (effects) {
		case NO_EFFECTS:
			mAdditionEffect = null;
			break;
		case SCALE_FROM_TOP_EFFECT:
			mAdditionEffect = new ScaleFromTopEffect();
			break;
		case SCALE_FROM_LEFT_EFFECT:
			mAdditionEffect = new ScaleFromLeftEffect();
			break;
		case SCALE_FROM_TOP_LEFT_EFFECT:
			mAdditionEffect = new ScaleFromTopLeftEffect();
			break;
		case SCALE_FROM_TOP_RIGHT_EFFECT:
			mAdditionEffect = new ScaleFromTopRightEffect();
			break;
		default:
			break;
		}
	}

	private void notifyListViewItems(final ListView listView,
			final ListAdapter adapter) {
		int firstVisiblePosition = listView.getFirstVisiblePosition();
		for (int i = 0; i < listView.getChildCount(); i++) {
			adapter.getView(firstVisiblePosition + i, listView.getChildAt(i),
					listView);
		}
	}
}
