package com.example.amap3d.views;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.amap3d.MainActivity;
import com.example.amap3d.R;

import java.util.List;
import java.util.Map;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.ViewHolder> {

    private List<Map<String, String>> timetableList;

    public TimetableAdapter(List<Map<String, String>> timetableList) {
        this.timetableList = timetableList;
    }

    @Override
    public TimetableAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(MainActivity.getActivity()).inflate(R.layout.item_timetable, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TimetableAdapter.ViewHolder holder, int position) {
        Map<String, String> timetable = timetableList.get(position);
        holder.timeTextView.setText(timetable.get("timeList"));
        holder.dateTextView.setText(timetable.get("timeTitle"));
        holder.placeTextView.setText(timetable.get("routeTitle"));
    }

    @Override
    public int getItemCount() {
        return timetableList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView placeTextView;
        TextView dateTextView;
        TextView timeTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            placeTextView = itemView.findViewById(R.id.placeTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }
}
