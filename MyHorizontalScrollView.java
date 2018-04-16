package com.ibgoing.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ibgoing.adapter.HorizontalScrollViewAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 横向广告控件
 *
 * @author yxm
 */
public class MyHorizontalScrollView extends HorizontalScrollView implements OnClickListener, RollRelativeLayout.StopViewRoll {
    private int startScroll = 0;
    /**
     * 检查是否滚动handler
     **/
    private Handler mCheckHandler;
    /**
     * 手动停止标示
     **/
    private boolean isDownStop = false;
    /**
     * 停止状态，回到桌面可能改变着值
     **/
    private boolean isStop = false;
    /**
     * 子view的margin
     **/
    private int mRedundantMargSize = 0;
    /**
     * 子view的paddingre
     **/
    private int mRedundantPaddingSize = 0;

    /**
     * 图片滚动时的回调接口
     */
    public interface CurrentImageChangeListener {
        void onCurrentImgChanged(int position, View viewIndicator);
    }

    /**
     * 条目点击时的回调
     */
    public interface OnItemClickListener {
        void onClick(View view, int pos);
    }

    private CurrentImageChangeListener mListener;

    private OnItemClickListener mOnClickListener;

    private static final String TAG = "MyHorizontalScrollView";

    /**
     * HorizontalListView中的LinearLayout
     */
    private LinearLayout mContainer;

    /**
     * 子元素的宽度
     */
    private int mChildWidth;
    /**
     * 子元素的高度
     */
    private int mChildHeight;
    /**
     * 当前第一张图片的下标
     */
    private int mFristIndex;
    /**
     * 当前最后一张图片的index(有可能是不可见的view)
     */
    private int mCurrentIndex;
    /**
     * 当前第一个View
     */
    private View mFirstView;
    /**
     * 数据适配器
     */
    private HorizontalScrollViewAdapter mAdapter;
    /**
     * 每屏幕最多显示的个数
     */
    private int mCountOneScreen;
    /**
     * 屏幕的宽度
     */
    private int mScreenWitdh;

    private boolean isCanScroll = true;

    /**
     * 保存View与位置的键值对
     */
    private Map<View, Integer> mViewPos = new HashMap<View, Integer>();
    /**
     * 多余的宽度
     **/
    private int mRedundantWidth = 0;

    /**
     * 删除的viewList
     */
    private ArrayList<View> mDeleteViewList = new ArrayList<>();


    /**
     * 自动滚动hander
     **/
    private Handler mRollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            startSmoothneRoll();
        }
    };
    /**
     * 自动滚动线程
     **/
    private Thread mRollThread;

    public MyHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 获得屏幕宽度
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWitdh = outMetrics.widthPixels;
        mCheckHandler = new Handler();
        setEnabled(true);
        setClickable(true);
    }

    /**
     * 取消线程
     */
    public void cancelThread() {
        if (mRollHandler != null && runnable != null) {
            mRollHandler.removeCallbacks(runnable);
            mRollHandler = null;
        }

        if (mCheckHandler != null && checkRunAble != null) {
            mCheckHandler.removeCallbacks(checkRunAble);
            mCheckHandler = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //设置未能整除的宽度作为宽度
        if (mRedundantWidth != 0 && mChildHeight != 0) {
            setMeasuredDimension(mScreenWitdh - mRedundantWidth, mChildHeight);
            mRedundantWidth = 0;
        }
    }


    /**
     * 初始化数据，设置数据适配器
     *
     * @param mAdapter
     */
    public void initDatas(HorizontalScrollViewAdapter mAdapter) {
        //数据为空
        if (mAdapter.getDataCount() == 0) {
            return;
        }
        // 能看到view的个数
        int screenCount = 0;
        this.mAdapter = mAdapter;
        mContainer = (LinearLayout) getChildAt(0);
        mContainer.removeAllViews();
        // 获得适配器中第一个View 需固定子宽高
        final View view = mAdapter.getView(0, null, mContainer);
        mContainer.addView(view);
        // 强制计算当前View的宽和高
        if (mChildWidth == 0 && mChildHeight == 0) {
            int w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            int h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            view.measure(w, h);
            mChildHeight = view.getMeasuredHeight();
            mChildWidth = view.getMeasuredWidth();
            Log.e(TAG, view.getMeasuredWidth() + "," + view.getMeasuredHeight());
            if (mChildWidth == 0) {
                mChildWidth = mScreenWitdh / 5;
                return;
            }
            // 多出来的空隙
            int redundantSize = mScreenWitdh % mChildWidth;
            mCountOneScreen = (mScreenWitdh / mChildWidth == 0) ? mScreenWitdh / mChildWidth + 1 : mScreenWitdh / mChildWidth + 2;
            //不满一屏和刚好满一屏的情况
            if ((mCountOneScreen - 2) >= mAdapter.getDataCount()) {
                //刚好满一屏 但间隙未达到规定的值 则设置可以滑动,后续操作会扩大间隙
                if ((mCountOneScreen - 2) == mAdapter.getDataCount() && redundantSize < mAdapter.getDataCount() * 6) {
                    isCanScroll = true;
                    screenCount = mCountOneScreen - 2;
                } else {
                    //不满一屏
                    isCanScroll = false;
                    mCountOneScreen = mAdapter.getDataCount();
                    screenCount = mCountOneScreen;
                }
            } else {
                screenCount = mCountOneScreen - 2;
            }
            // 设置linearLayout间隙
            int totalWidth;
            // 如果小于规定的间隙 则减少一个view做间隙 isCanScroll不满一屏时不操作
            if (isCanScroll && redundantSize < 6 * screenCount)// 规定要每个view要有6的边距
            {
                mCountOneScreen--;
                screenCount--;
                totalWidth = redundantSize + mChildWidth;

            } else {
                totalWidth = redundantSize;
            }
            int redundantWidth = totalWidth % screenCount;
            if (redundantWidth != 0) {
                // 整除不了则需要重新设置空间（父控件）长度
                mRedundantWidth = redundantWidth;
            }
            int width = totalWidth / screenCount;
            mRedundantMargSize = width / 2;
            mChildWidth = mChildWidth + width;
            Log.e(TAG, "mCountOneScreen = " + mCountOneScreen + " ,mChildWidth = " + mChildWidth + ",mRedundantWidth=" + mRedundantWidth);

        }
        // 初始化第一屏幕的元素
        initFirstScreenChildren(mCountOneScreen);

        try {
            // 去除滑动到底部渐变颜色
            Method method = getClass().getMethod("setOverScrollMode", int.class);
            Field field = getClass().getField("OVER_SCROLL_NEVER");
            if (method != null && field != null) {
                method.invoke(this, field.getInt(View.class));
            }
        } catch (Exception e) {
        }
    }

    /**
     * 加载第一屏的View
     *
     * @param mCountOneScreen
     */
    public void initFirstScreenChildren(int mCountOneScreen) {
        mContainer = (LinearLayout) getChildAt(0);
        mContainer.removeAllViews();
        mViewPos.clear();
        for (int i = 0; i < mCountOneScreen; i++) {
            View childView = getChildView(i);
            mContainer.addView(childView);
            mViewPos.put(childView, i);
            mCurrentIndex = i;
        }
        scrollTo(0, 0);
        if (mListener != null) {
            notifyCurrentImgChanged();
        }
        // 是否开启自动滚动
        if (isCanScroll) {
            startRollView(1000);
        }
    }

    /**
     * 加载下一张图片
     *
     * @param scrollX
     */
    protected void loadNextImg(int scrollX) {
        // 数组边界值计算
        // if (mCurrentIndex == mAdapter.getCount() - 1) {
        // return;
        // }
        // 移除第一个
        removeChildView(0);

        // 获取下一张图片，并且设置onclick事件，且加入容器中
        View childView = getChildView(++mCurrentIndex);
        mContainer.addView(childView);
        mViewPos.put(childView, mCurrentIndex);
        // 移除第一张图片，且将水平滚动位置置0
        // 可能屏幕的最前方会有两个缓存view（不可见的）这时会错位
        // 有bug scrollX有时会少几个像素 这里误差处理
        if (scrollX > (mChildWidth * 1.5)) {
            scrollTo(mChildWidth, 0);

        } else {
            // 前面只有一个缓存view
            scrollTo(0, 0);

        }
        // 当前第一张图片小标
        mFristIndex++;
        // 如果设置了滚动监听则触发
        if (mListener != null) {
            notifyCurrentImgChanged();
        }

    }

    /**
     * 加载前一张图片
     */
    protected void loadPreImg() {
        // 为实现左边无限滑动 所有index可能为负 需注意越界的处理方法
        int index = mCurrentIndex - mCountOneScreen;

        // 移除最后一张
        int oldViewPos = mContainer.getChildCount() - 1;
        removeChildView(oldViewPos);
        // 将此View放入第一个位置
        View view = getChildView(index);
        mViewPos.put(view, index);
        mContainer.addView(view, 0);
        view.setOnClickListener(this);
        // 水平滚动位置向左移动view的宽度个像素
        scrollTo(mChildWidth, 0);
        // 当前位置--，当前第一个显示的下标--
        mCurrentIndex--;
        mFristIndex--;
        // 回调
        if (mListener != null) {
            notifyCurrentImgChanged();
        }

    }

    /**
     * 滑动时的回调
     */
    public void notifyCurrentImgChanged() {
        mListener.onCurrentImgChanged(mFristIndex, mContainer.getChildAt(0));

    }

    private void showLog() {
        // 获得当前应该显示为第一张图片的下标
        ViewGroup vg = (ViewGroup) ((ViewGroup) getChildAt(0)).getChildAt(0);
        ViewGroup vg1 = (ViewGroup) ((ViewGroup) getChildAt(0)).getChildAt(((ViewGroup) ((ViewGroup) getChildAt(0))).getChildCount() - 1);
        Log.e(TAG, "getScrollX---" + getScrollX() + "   " + ((ViewGroup) getChildAt(0)).getChildCount() + "个孩子" + "第一个孩子文字" + ((TextView) vg.getChildAt(1)).getText().toString() + "---最后个孩子文字" + ((TextView) vg1.getChildAt(1)).getText().toString());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 是否允许滑动
        if (!isCanScroll || mChildWidth == 0) {
            return true;
        }
        //showLog();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isStopRoll()) {
                    stopRoll();
                }
                int scrollX = getScrollX();
                if (scrollX >= mChildWidth) {
                    loadNextImg(scrollX);
                }
                // 如果当前scrollX = 0， 往前设置一张，移除最后一张
                if (scrollX == 0) {
                    loadPreImg();
                }
                break;
            case MotionEvent.ACTION_UP:
                isDownStop = false;
                isStop = false;
                mCheckHandler.post(checkRunAble);
                break;
        }
        return super.onTouchEvent(ev);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 调整view的坐标
     **/
    private void adjustView() {
        if (mChildWidth == 0) {
            return;
        }
        int endScroll = getScrollX();
        int scroll = endScroll % mChildWidth;
        if (scroll == 0) {
            return;
        }
        if (scroll >= mChildWidth / 2) {
            smoothScrollBy(mChildWidth - scroll, 0);
        } else {
            smoothScrollBy(-scroll, 0);
        }
        if (mCheckHandler != null) {
            mCheckHandler.removeCallbacks(checkRunAble);
        }
        /** 点击之后就不自动滚动 **/
        // if (mRollHandler != null) {
        // mRollHandler.removeCallbacks(rollRunAble);
        // mRollHandler.postDelayed(rollRunAble, 1000);
        // }
    }

    @Override
    public void onClick(View v) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(v, mViewPos.get(v));
        }


    }

    /**
     * 重置选中状态
     */
/*    public void resetSelectState() {
        if (mContainer == null) {
            return;
        }
        for (int i = 0; i < mContainer.getChildCount(); i++) {
            ViewGroup viewGroup = (ViewGroup) mContainer.getChildAt(i);
            View view = viewGroup.findViewById(R.id.wish_gallery_item_image_bg);
            if (view != null) {
                view.setVisibility(View.INVISIBLE);
            }
        }
    }*/
    public void setOnItemClickListener(OnItemClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    public void setCurrentImageChangeListener(CurrentImageChangeListener mListener) {
        this.mListener = mListener;
    }

    // 每隔5毫秒判断是否滚动
    Runnable checkRunAble = new Runnable() {
        @Override
        public void run() {
            int endScroll = getScrollX();
            if (endScroll == startScroll) {
                adjustView();
                isDownStop = true;
            } else {
                mCheckHandler.postDelayed(this, 5);
                startScroll = endScroll;
            }
        }
    };
    Runnable rollRunAble = new Runnable() {
        @Override
        public void run() {
            // isDownStop（用户手势）优先级比较高 若为true则不执行滚动
            if (!isDownStop && !isStop) {
                //rollNextView();
                //RollHandler.postDelayed(rollRunAble, 2000);
            }
        }

    };


    /**
     * 末尾添加一个view
     */
    private View getChildView(int pos) {
        View oldView = null;
        //L.i("DeleteViewListSize-------------",mDeleteViewList.size()+"");
        //从删除的view集合里面取
        if (mDeleteViewList.size() > 0) {
            oldView = mDeleteViewList.get(0);
            mDeleteViewList.remove(0);
        }
        // 获取下一个，并且设置onclick事件，且加入容器中
        View childView = mAdapter.getView(pos, oldView, mContainer);
        childView.setOnClickListener(this);
        // 设置停止滚动回调
        if (childView instanceof RollRelativeLayout) {
            RollRelativeLayout rollRelativeLayout = (RollRelativeLayout) childView;
            rollRelativeLayout.setStopListener(this);
            setChildRedundant(rollRelativeLayout);
        }
        return childView;
    }

    /**
     * 删除一个view
     *
     * @param pos 删除view的下标
     */
    private void removeChildView(int pos) {
        ViewGroup childView = (ViewGroup) mContainer.getChildAt(pos);
        mViewPos.remove(childView);
        mContainer.removeViewAt(pos);
        if (mDeleteViewList.size() >= 5) {
            mDeleteViewList.clear();
        }
        mDeleteViewList.add(childView);
    }

    /**
     * 设置子view内外边距
     *
     * @param childView
     */
    private void setChildRedundant(RollRelativeLayout childView) {
        if (mRedundantMargSize != 0) {
            childView.setMarginWidth(mRedundantMargSize);
        }
        if (mRedundantPaddingSize != 0) {
            childView.setPadingWidth(mRedundantPaddingSize);
        }
    }


    /**
     * 手动滚动-下一项
     */
    public void scrollToNext() {
        if (!isStop) {
            stopRoll();
        }
        if (!isCanScroll) {
            return;
        }
        // smoothScrollBy(mChildWidth, 0);
        int scrollx = getScrollX();
        if (scrollx > 0 && scrollx < mChildWidth) {
            int x = mChildWidth - scrollx;
            smoothScrollBy(x, 0);
        } else {
            rollNextView();
        }
    }

    /**
     * 手动滚动-上一项
     */
    public void scrollToPrevious() {
        if (!isStop) {
            stopRoll();
        }
        if (!isCanScroll) {
            return;
        }
        int scrollx = getScrollX();
        if (scrollx == 0) {
            rollPreviousView();
        } else {
            smoothScrollBy(-mChildWidth, 0);
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    if (!isDownStop && !isStop) {
                        Thread.sleep(15);
                        mRollHandler.sendEmptyMessage(0);
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }
    };

    /**
     * 开始滚动
     *
     * @param millis 多少秒后开启
     */
    public void startRollView(int millis) {
        if (mRollHandler != null) {
            isStop = false;
            // mRollHandler.removeCallbacks(rollRunAble);
            //mRollHandler.postDelayed(rollRunAble, 1000);
            mRollThread = new Thread(runnable);
            mRollThread.start();
        }
    }

    /**
     * 自动滚动-平滑滚动
     */
    private void startSmoothneRoll() {
        if (this.getChildCount() == 0 || mChildWidth == 0) {
            return;
        }
        int scroll = getScrollX();
        if (scroll >= mChildWidth) {
            removeChildView(0);
            View childView = getChildView(++mCurrentIndex);
            mContainer.addView(childView);
            mViewPos.put(childView, mCurrentIndex);
            scrollTo(0, 0);
            // 如果设置了滚动监听则触发
            if (mListener != null) {
                notifyCurrentImgChanged();
            }
            // 当前第一张图片小标
            mFristIndex++;
        }
        smoothScrollBy(2, 0);
    }

    /**
     * 停止滚动 - 可能再次滚动
     */
    public void stopRollView() {
        if (mRollHandler != null) {
            isStop = true;
            mRollHandler.removeCallbacks(rollRunAble);
        }
    }

    /**
     * 滚动到下个view
     */
    private void rollNextView() {
        if (this.getChildCount() == 0 || mChildWidth == 0) {
            return;
        }
        int scroll = getScrollX();
        // 滑动前左边没有子元素，这时需添加一个元素 否则会没滑动效果
        if (scroll == 0) {
            View view = getChildView(0);
            mContainer.addView(view, 0);
            // 保持mCountOneScreen+2个view
            int viewCount = mContainer.getChildCount();
            if (viewCount > mCountOneScreen) {
                // 需要remove的view个数
                int cutCount = viewCount - mCountOneScreen;
                for (int i = 0; i < cutCount; i++) {
                    int pos = viewCount - i - 1;
                    removeChildView(pos);
                    --mCurrentIndex;
                }

            }
        }// 左边有两个子元素 这时需删除一个 并在末尾添加一个
        else if (scroll >= (mChildWidth * 1.5)) {
            removeChildView(0);
            View childView = getChildView(++mCurrentIndex);
            mContainer.addView(childView);
            mViewPos.put(childView, mCurrentIndex);
        }
        removeChildView(0);
        View childView = getChildView(++mCurrentIndex);
        mContainer.addView(childView);
        mViewPos.put(childView, mCurrentIndex);
        scrollTo(0, 0);
        smoothScrollBy(mChildWidth, 0);

        // 如果设置了滚动监听则触发
        if (mListener != null) {
            notifyCurrentImgChanged();
        }
        // 当前第一张图片小标
        mFristIndex++;
    }

    /**
     * 滑动到上一个view
     */
    private void rollPreviousView() {
        if (this.getChildCount() == 0 || mChildWidth == 0) {
            return;
        }
        // showLog();
        int scroll = getScrollX();
        // 为实现左边无限滑动 所有index可能为负 需注意越界的处理方法
        int index = mCurrentIndex - mCountOneScreen;
        // 移除最后一张
        int oldViewPos = mContainer.getChildCount() - 1;
        removeChildView(oldViewPos);

        // 将此View放入第一个位置
        View view = getChildView(index);
        mViewPos.put(view, index);
        mContainer.addView(view, 0);
        view.setOnClickListener(this);
        if (scroll == mChildWidth) {
            scrollTo(mChildWidth * 2, 0);
        } else if (scroll == 0) {
            scrollTo(mChildWidth, 0);
        }

        smoothScrollBy(-mChildWidth, 0);
        // 当前位置--，当前第一个显示的下标--
        mCurrentIndex--;
        mFristIndex--;
        // 回调
        if (mListener != null) {
            notifyCurrentImgChanged();

        }
    }

    /**
     * 是否停止滚动
     *
     * @return
     */
    public boolean isStopRoll() {
        return isStop;
    }

    /**
     * 设置不可能再次滚动
     */
    public void setDownStop() {
        isDownStop = true;
    }

    /**
     * 停止滚动 - 不可能再次滚动
     */
    @Override
    public void stopRoll() {
        if (!isStopRoll()) {
            stopRollView();
            isDownStop = true;
        }
    }
}
