package com.fenchtose.pixcart;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class OverlayView extends View {

	public OverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override 
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
	   int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
	   int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
	   this.setMeasuredDimension(parentWidth, parentHeight - parentWidth);
	   RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.getLayoutParams();
	   params.height = parentHeight - parentWidth;
	   params.width = parentWidth;
	   this.setLayoutParams(params);
//	   this.setLayoutParams(new *ParentLayoutType*.LayoutParams(parentWidth/2,parentHeight));
	   super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
}