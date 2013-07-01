package com.android.sickfuture.sickcore.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseCursorAdapter extends CursorAdapter {

	
	public BaseCursorAdapter(Context context, Cursor c) {
        super(context, c, true);
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid) {
                    throw new IllegalStateException(
                                    "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                    throw new IllegalStateException("couldn't move cursor to position "
                                    + position);
            }
            View view;
            if (convertView == null) {
                    view = newView(mContext, mCursor, parent);
                    ViewHolder holder = getViewHolder(view);
                    view.setTag(holder);
            } else {
                    view = convertView;
            }
            bindView(view, mContext, mCursor);
            return view;
    }

	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		bindData(view, context, cursor, holder);
	}
	
	abstract public void bindData(View view, Context context, Cursor cursor, ViewHolder holder);
	
	protected ViewHolder getViewHolder(View view){
		ViewHolder holder = new ViewHolder();
		int[] ids = getViewsIds();
		for (int i = 0; i<ids.length; i++){
			holder.add(ids[i], view.findViewById(ids[i]));
		}
		return holder;
	}
	
	abstract protected int[] getViewsIds();
	
	protected class ViewHolder{
		SparseArray<View> views;
		public ViewHolder(){
			views = new SparseArray<View>();
		}
		public void add(int id, View view){
			views.put(id, view);
		}
		
		public View getViewById(int id){
			return views.get(id);
		}
	}

	

}
