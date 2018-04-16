package com.ibgoing.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 滚动适配器
 */
public class HorizontalScrollViewAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private List<DynamicData> mPrizeList;

    public HorizontalScrollViewAdapter(Context context, List<DynamicData> prizeList) {
        this.mContext = context;
        mInflater = LayoutInflater.from(context);
        this.mPrizeList = prizeList;
    }

    public int getCount() {
        return mPrizeList == null ? 0 : Integer.MAX_VALUE;
    }

    public int getDataCount() {
        return mPrizeList == null ? 100 : mPrizeList.size();
    }

    public Object getItem(int position) {
        int count = getDataCount();
        // position为负则说明到了起始位置，需特殊处理
        if (position < 0) {
            // -1则返回 list的最后一个
            int pos = Math.abs(count + position);
            return mPrizeList.get(pos % count);
        } else {
            return mPrizeList.get(position % count);
        }

    }

    public int getItemId(int position) {
        int count = getDataCount();
        if (position < 0) {
            int pos = Math.abs(mPrizeList.size() + position);
            // 有可能出现 3210123这样的序列 期望是 1230123
            if (Math.abs(position) < count || pos % count == 0)
                // -1+3 = 2(List的索引，既List的最后一个) -2+3 = 1(第二个)
                return pos % count;
            else {
                // 0+0=0 ，abs(-4+3) = 1 此时不是需要的索引需要2
                return count - (pos % count);
            }
        } else {
            return position % mPrizeList.size();
        }
    }

	/**
		模型 数据处理
	**/
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.view_rentalinfo, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
		
        return convertView;
    }

    public class ViewHolder {
        @Bind(R.id.ci_head_photo)
        CircleImageView ciHeadPhoto;
        @Bind(R.id.iv_headpic)
        ImageView ivHeadpic;
        @Bind(R.id.iv_level)
        ImageView ivLevel;
        @Bind(R.id.tv_name)
        TextView tvName;
        @Bind(R.id.tv_car_state)
        TextView tvState;
        @Bind(R.id.tv_time)
        TextView tvTime;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

}
