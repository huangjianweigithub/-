import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class RollRelativeLayout extends RelativeLayout {

	public RollRelativeLayout(Context context) {
		super(context);
	}

	public RollRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		setClickable(true);
		//setBackgroundColor(Color.TRANSPARENT);
	}

	public RollRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			if (mStopViewRoll != null) {
			//	mStopViewRoll.stopRoll();
			}
			break;

		default:
			break;
		}
		// getParent().requestDisallowInterceptTouchEvent(true);
		// ((ViewGroup) getParent()).onTouchEvent(event);
		// postInvalidate();

		return super.onTouchEvent(event);
	}

	private StopViewRoll mStopViewRoll;

	public interface StopViewRoll {
		public void stopRoll();
	}

	public void setStopListener(StopViewRoll stopListener) {
		mStopViewRoll = stopListener;
	}

	/**
	 * 设置外边距
	 * 
	 * @param mRedundantSize
	 */
	public void setMarginWidth(int mRedundantSize) {
		MarginLayoutParams mLayoutParams = (MarginLayoutParams) getLayoutParams();
		mLayoutParams.leftMargin = mRedundantSize;
		mLayoutParams.rightMargin = mRedundantSize;
		setLayoutParams(mLayoutParams);
	}

	/**
	 * 设置内边距
	 * 
	 * @param mRedundantSize
	 */
	public void setPadingWidth(int mRedundantSize) {
		setPadding(mRedundantSize, 0, mRedundantSize, 0);
	}
}
