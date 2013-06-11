package com.android.sickfuture.sickcore.collections;

import java.util.ArrayList;

public class SetArrayList<E> extends ArrayList<E>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8780964491894192845L;
	
	@Override
	public void add(int index, E e){
		for (int i = 0; i<this.size(); i++){
			if (this.get(i).equals(e)){
				this.remove(i);
			}
		}
		super.add(index, e);
	}
}
