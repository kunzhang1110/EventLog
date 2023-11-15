package com.kunzhang1110.eventlog;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kunzhang1110.eventlog.models.RowData;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private final DateTimeFormatter dateTimeFormatter;
    private List<RowData> rowDataList = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView appName;
        private final ImageView appIcon;
        private final TextView time;
        private final TextView additional;

        public ViewHolder(View view) {
            super(view);
            appName = view.findViewById(R.id.textview_row_app_name);
            appIcon = view.findViewById(R.id.imageview_row_app_icon);
            time = view.findViewById(R.id.textview_row_time);
            additional = view.findViewById(R.id.textview_row_additional);
        }

        public TextView getAppName() {
            return appName;
        }

        public ImageView getAppIcon() {
            return appIcon;
        }

        public TextView getTime() {
            return time;
        }

        public TextView getAdditional() {
            return additional;
        }
    }

    public ListAdapter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        RowData usageHistory = rowDataList.get(position);
        Long durationInSeconds = usageHistory.durationInSeconds;

        if (durationInSeconds != null) {
            int textColor = (durationInSeconds >= 1800) ? Color.RED : Color.BLACK;
            viewHolder.getAppName().setTextColor(textColor);
            viewHolder.getAdditional().setTextColor(textColor);
        }

        viewHolder.getAppName().setText(usageHistory.appName);
        viewHolder.getTime().setText(dateTimeFormatter.format(usageHistory.time));
        viewHolder.getAdditional().setText(rowDataList.get(position).additional);
        viewHolder.getAppIcon().setImageDrawable(usageHistory.appIcon);
    }

    @Override
    public int getItemCount() {
        return rowDataList.size();
    }

    public void setRowDataList(List<RowData> rowDataList) {
        this.rowDataList = rowDataList;
    }
}
