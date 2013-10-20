package com.android.sickfuture.sickcore.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.android.sickfuture.sickcore.R;

public class CellLayout extends ViewGroup {

	private static final float DEFAULT_CELL_SIZE = 50;
	private float mCellSize;

	private static final int DEFAULT_COLUMNS_COUNT = 2;
	private int mColumnsCount;

	private static final int DEFAULT_SPACING = 0;
	private int mSpacing;

	public CellLayout(Context context) {
		super(context);
	}

	public CellLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public CellLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.CellLayout, 0, 0);
		try {
			mColumnsCount = a.getInt(R.styleable.CellLayout_columns,
					DEFAULT_COLUMNS_COUNT);
			mSpacing = a.getDimensionPixelSize(R.styleable.CellLayout_spacing,
					DEFAULT_SPACING);
		} finally {
			a.recycle();
		}
		mCellSize = DEFAULT_CELL_SIZE;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		int width = 0;
		int height = 0;

		if (widthMode == MeasureSpec.AT_MOST
				|| widthMode == MeasureSpec.EXACTLY) {
			width = MeasureSpec.getSize(widthMeasureSpec);
			mCellSize = (float) (getMeasuredWidth() - getPaddingLeft() - getPaddingRight())
					/ (float) mColumnsCount;
		} else {
			mCellSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					DEFAULT_CELL_SIZE, getResources().getDisplayMetrics());
			width = (int) (mColumnsCount * mCellSize);
		}

		int childCount = getChildCount();
		View child;

		int maxRow = 0;

		for (int i = 0; i < childCount; i++) {
			child = getChildAt(i);

			LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

			int top = layoutParams.top;
			int w = layoutParams.width;
			int h = layoutParams.height;

			int bottom = top + h;

			int childWidthSpec = MeasureSpec.makeMeasureSpec(
					(int) (w * mCellSize) - mSpacing * 2, MeasureSpec.EXACTLY);
			int childHeightSpec = MeasureSpec.makeMeasureSpec(
					(int) (h * mCellSize) - mSpacing * 2, MeasureSpec.EXACTLY);
			child.measure(childWidthSpec, childHeightSpec);

			if (bottom > maxRow) {
				maxRow = bottom;
			}
		}

		int measuredHeight = Math.round(maxRow * mCellSize) + getPaddingTop()
				+ getPaddingBottom();
		if (heightMode == MeasureSpec.EXACTLY) {
			height = MeasureSpec.getSize(heightMeasureSpec);
		} else if (heightMode == MeasureSpec.AT_MOST) {
			int atMostHeight = MeasureSpec.getSize(heightMeasureSpec);
			height = Math.min(atMostHeight, measuredHeight);
		} else {
			height = measuredHeight;
		}
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		int childCount = getChildCount();
		View childView;
		for (int i = 0; i < childCount; i++) {
			childView = getChildAt(i);

			LayoutParams layoutParams = (LayoutParams) childView
					.getLayoutParams();

			int childTop = (int) (layoutParams.top * mCellSize)
					+ getPaddingTop() + mSpacing;
			int childLeft = (int) (layoutParams.left * mCellSize)
					+ getPaddingLeft() + mSpacing;
			int childRight = (int) ((layoutParams.left + layoutParams.width) * mCellSize)
					+ getPaddingLeft() - mSpacing;
			int childBottom = (int) ((layoutParams.top + layoutParams.height) * mCellSize)
					+ getPaddingTop() - mSpacing;

			childView.layout(childLeft, childTop, childRight, childBottom);
		}
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(
			ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {

		int top = 0;
		int left = 0;

		int width = 1;
		int height = 1;

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray a = null;
			try {
				a = context.obtainStyledAttributes(attrs,
						R.styleable.CellLayout);
				left = a.getInt(R.styleable.CellLayout_layout_left, 0);
				top = a.getInt(R.styleable.CellLayout_layout_top, 0);
				height = a
						.getInt(R.styleable.CellLayout_layout_cellsHeight, -1);
				width = a.getInt(R.styleable.CellLayout_layout_cellsWidth, -1);
			} finally {
				if (a != null) {
					a.recycle();
				}
			}
		}

		public LayoutParams(ViewGroup.LayoutParams params) {
			super(params);
			if (params instanceof LayoutParams) {
				LayoutParams cellLayoutParams = (LayoutParams) params;
				left = cellLayoutParams.left;
				top = cellLayoutParams.top;
				height = cellLayoutParams.height;
				width = cellLayoutParams.width;
			}
		}

		public LayoutParams() {
			this(MATCH_PARENT, MATCH_PARENT);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

	}

}
