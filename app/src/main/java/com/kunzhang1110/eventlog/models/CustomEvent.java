package com.kunzhang1110.eventlog.models;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomEvent {

    public String appName;
    public Drawable appIcon;
    public String eventType;
    public LocalDateTime time;

    public final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NonNull
    @Override
    public String toString() {
        return String.format("%s, %s, %s", appName, time.format(dateTimeFormatter), eventType);
    }

}