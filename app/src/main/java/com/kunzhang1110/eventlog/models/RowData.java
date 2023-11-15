package com.kunzhang1110.eventlog.models;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RowData implements Comparable<RowData> {

    public String appName;
    public Drawable appIcon;
    public Long durationInSeconds = 0L;
    public LocalDateTime time;
    public String additional;
    public final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RowData(String appName, Drawable appIcon, LocalDateTime time, String additional) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.time = time;
        this.additional = additional;
    }

    @Override
    public int compareTo(RowData otherRowData) {
        return time.compareTo(otherRowData.time);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s, %s, %s", appName, time.format(dateTimeFormatter), additional);
    }
}
