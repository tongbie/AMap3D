package com.example.amap3d.views.Timetable;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.gsons.TodayTimetableGson;

import java.util.List;

public class NextBusAdapter extends RecyclerView.Adapter<NextBusAdapter.ViewHolder> {
    private Context context;
    private List<TodayTimetableGson> todayTimetableGsonList;
    private boolean isShowAll = true;

    public NextBusAdapter(List<TodayTimetableGson> todayTimetableGsonList, Context context) {
        this.todayTimetableGsonList = todayTimetableGsonList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_timetable, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (isShowAll){

        }else {

        }
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
