package com.example.amap3d.views;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.amap3d.R;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class SelectView extends LinearLayout {
    private List<Integer> idList;
    private List<String> nameList;

    private int itemNum;
    private ItemView selectedItemView;
    private List<ItemView> itemViewList = new ArrayList<>();

    private OnSelectListener onSelectListener;

    public void initData(List<Integer> idList, List<String> nameList) throws Exception {
        this.idList = idList;
        this.nameList = nameList;
        if (idList.size() != nameList.size()) {
            throw new Exception("length not equal");
        }
        itemNum = nameList.size();
        Datas.setCurrentSelectorId(idList.get(0));
        addView();
    }

    private void init() {
        this.setOrientation(HORIZONTAL);
        this.setBackground(getResources().getDrawable(R.drawable.bg_shadow));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.setElevation(Utils.px(getContext(), 4));
        }
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.height = Utils.px(getContext(), 45);
        this.setLayoutParams(layoutParams);
    }

    private void addView() {
        itemViewList.clear();
        for (int i = 0; i < nameList.size(); i++) {
            final ItemView itemView = new ItemView(getContext(), idList.get(i), nameList.get(i));
            itemViewList.add(itemView);
            selectedItemView = itemViewList.get(0);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onSelectListener != null && !itemView.isClicked()) {
                        onSelectListener.onSelect(itemView.getId());
                        itemView.setClicked(true);
                        selectedItemView.setClicked(false);
                        Datas.setCurrentSelectorId(itemView.getId());
                    }
                    selectedItemView = itemView;
                }
            });
            SelectView.this.addView(itemView);
        }
        if (itemNum > 0) {
            itemViewList.get(0).setClicked(true);
            int viewWidth = Utils.getScreenWidth(getContext()) - Utils.px(getContext(), 16);//屏幕宽度-margin
            int itemWidth = viewWidth / itemNum;
            for (ItemView itemView : itemViewList) {
                LayoutParams layoutParams = (LayoutParams) itemView.getLayoutParams();
                layoutParams.width = itemWidth;
                itemView.setLayoutParams(layoutParams);
            }
        }
    }

    public interface OnSelectListener {
        void onSelect(int id);
    }

    public void setOnSelectListener(OnSelectListener onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    private class ItemView extends android.support.v7.widget.AppCompatTextView {
        private int id;
        private boolean isClicked = false;

        public ItemView(Context context, int id, String name) {
            super(context);
            this.id = id;
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            this.setLayoutParams(layoutParams);
            this.setGravity(Gravity.CENTER);
            this.setTextColor(Color.parseColor("#666666"));
            this.setTextSize(16);
            this.setText(name);
        }

        public void setClicked(boolean isClicked) {
            if (isClicked) {
                this.setTextColor(Color.parseColor("#3296fa"));
            } else {
                this.setTextColor(Color.parseColor("#666666"));
            }
            this.isClicked = isClicked;
        }

        public int getId() {
            return id;
        }

        public boolean isClicked() {
            return isClicked;
        }
    }

    /*--------------------------------------------------------------------------------------------*/

    public SelectView(Context context) {
        super(context);
        init();
    }

    public SelectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SelectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
}
