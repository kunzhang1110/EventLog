package com.kunzhang1110.eventlog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kunzhang1110.eventlog.models.CustomEvent;
import com.kunzhang1110.eventlog.models.RowData;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private UsageStatsManager usageStatsManager;
    private RecyclerView recyclerView;
    private ListAdapter listAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnConcise, btnVerbose, btnRaw;
    private final ArrayList<RowData> rawRowDataList = new ArrayList<>();
    private final ArrayList<RowData> verboseRowDataList = new ArrayList<>();
    private final ArrayList<RowData> conciseRowDataList = new ArrayList<>();
    private String CurrentRowDataFlag = "Verbose";

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<Integer, String> EVENT_TYPE_MAP = new HashMap<>();

    static {
        //These four types are used in calculate duration
        EVENT_TYPE_MAP.put(UsageEvents.Event.ACTIVITY_RESUMED, "Activity Resumed");
        EVENT_TYPE_MAP.put(UsageEvents.Event.ACTIVITY_STOPPED, "Activity Stopped");
        EVENT_TYPE_MAP.put(UsageEvents.Event.SCREEN_NON_INTERACTIVE, "Screen Non-Interactive");
        EVENT_TYPE_MAP.put(UsageEvents.Event.KEYGUARD_HIDDEN, "Keyguard Hidden");
        //The followings types are not used in calculate duration
        EVENT_TYPE_MAP.put(UsageEvents.Event.SCREEN_INTERACTIVE, "Screen Interactive");
        EVENT_TYPE_MAP.put(UsageEvents.Event.KEYGUARD_SHOWN, "Keyguard Shown");
        EVENT_TYPE_MAP.put(UsageEvents.Event.ACTIVITY_PAUSED, "Activity Paused");
        EVENT_TYPE_MAP.put(UsageEvents.Event.FOREGROUND_SERVICE_START, "Foreground Service Start");
        EVENT_TYPE_MAP.put(UsageEvents.Event.FOREGROUND_SERVICE_STOP, "Foreground Service Stop");
        EVENT_TYPE_MAP.put(UsageEvents.Event.DEVICE_STARTUP, "Device Startup");
        EVENT_TYPE_MAP.put(UsageEvents.Event.DEVICE_SHUTDOWN, "Device Shutdown");
        EVENT_TYPE_MAP.put(UsageEvents.Event.CONFIGURATION_CHANGE, "Configuration Change");
        EVENT_TYPE_MAP.put(UsageEvents.Event.SHORTCUT_INVOCATION, "Shortcut Invocation");
        EVENT_TYPE_MAP.put(UsageEvents.Event.USER_INTERACTION, "User Interaction");
    }

    private static final ArrayList<String> EVENT_TYPES_FOR_DURATION_LIST = //These four types are used in calculate duration
            new ArrayList<>(Arrays.asList("Activity Resumed", "Activity Stopped", "Screen Non-Interactive", "Keyguard Hidden"));
    private static final ArrayList<String> APP_NAME_EXCLUDED_LIST = //These Apps are excluded from Event List
            new ArrayList<>((Arrays.asList("Permission controller", "Pixel Launcher")));

    private static final Calendar CAL = Calendar.getInstance();
    private static final int DAYS = 2; //days in which events are included

    static {
        CAL.add(Calendar.DATE, -DAYS);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listAdapter = new ListAdapter(dateTimeFormatter);
        usageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        recyclerView = findViewById(R.id.app_usage_list);
        recyclerView.setAdapter(listAdapter);

        swipeRefreshLayout = findViewById(R.id.layout_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
                    updateList();
                    switch (CurrentRowDataFlag) {
                        case "Concise":
                            updateAdapter(conciseRowDataList);
                            break;
                        case "Verbose":
                            updateAdapter(verboseRowDataList);
                            break;
                        case "Raw":
                            updateAdapter(rawRowDataList);
                            break;
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }
        );

        btnConcise = findViewById(R.id.btn_concise);
        btnVerbose = findViewById(R.id.btn_verbose);
        btnRaw = findViewById(R.id.btn_raw);


        btnConcise.setOnClickListener(v -> {//only show each Screen Locked that is longer than 10 min. and the activity before it
            conciseRowDataList.clear();
            for (int i = 1; i < verboseRowDataList.size(); i++) {
                RowData rowData = verboseRowDataList.get(i);
                if (rowData.appName.equals("Screen Locked") & (rowData.durationInSeconds >= 600)) {
                    conciseRowDataList.add(verboseRowDataList.get(i - 1));
                    conciseRowDataList.add(rowData);
                }
            }
            updateAdapter(conciseRowDataList);
            highlightButton(btnConcise);
            CurrentRowDataFlag = "Concise";
        });
        btnVerbose.setOnClickListener(v -> {
            updateAdapter(verboseRowDataList);
            highlightButton(btnVerbose);
            CurrentRowDataFlag = "Verbose";
        });
        btnRaw.setOnClickListener(v -> {
            updateAdapter(rawRowDataList);
            highlightButton(btnRaw);
            CurrentRowDataFlag = "Raw";

        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> recyclerView.scrollToPosition(0));

        updateList();
        btnConcise.callOnClick(); //click btn concise
    }


    private void updateList() {

        UsageEvents usageEvents = usageStatsManager.queryEvents(CAL.getTimeInMillis(), System.currentTimeMillis());

        Map<String, ArrayList<CustomEvent>> customEventMap = new HashMap<>();
        verboseRowDataList.clear();
        rawRowDataList.clear();

        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);

            String packageName = event.getPackageName();
            String eventType = EVENT_TYPE_MAP.get(event.getEventType());
            if (eventType == null) continue;

            CustomEvent customEvent = new CustomEvent();
            customEvent.eventType = eventType;
            customEvent.time = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault());

            try {//get app name
                PackageManager packageManager = getPackageManager();
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String appName = (String) packageManager.getApplicationLabel(appInfo);
                if (APP_NAME_EXCLUDED_LIST.contains(appName)) continue;
                customEvent.appName = appName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("MyLog", String.format("App info is not found for %s", packageName));
                customEvent.appName = packageName;
            }

            try {//get app icon
                customEvent.appIcon = getPackageManager().getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("MyLog", String.format("App Icon is not found for %s", packageName));
                customEvent.appIcon = AppCompatResources.getDrawable(this, android.R.drawable.sym_def_app_icon);
            }
            Log.i("here", customEvent.toString());
//            if (!EVENT_TYPES_FOR_DURATION_LIST.contains(customEvent.eventType)) {
//
//            }
            //add to rawEventList
            rawRowDataList.add(new RowData(packageName, customEvent.appIcon, customEvent.time, customEvent.eventType));


            //add to eventsMap
            if (EVENT_TYPES_FOR_DURATION_LIST.contains(eventType)) {
                customEventMap.computeIfAbsent(customEvent.appName, k -> new ArrayList<>()).add(customEvent);
            }
        }

        for (Map.Entry<String, ArrayList<CustomEvent>> entry : customEventMap.entrySet()) {
            String appName = entry.getKey();
            ArrayList<CustomEvent> eventList = entry.getValue();

            for (int x = 0; x < eventList.size(); x++) {
                CustomEvent eventX = eventList.get(x);

                if (isResumedOrNonInteractive(eventX)) {
                    int y = x + 1;

                    while (y < eventList.size() && isResumedOrNonInteractive(eventList.get(y))) {
                        y++;
                    }

                    if (y < eventList.size()) {
                        CustomEvent eventY = eventList.get(y);
                        long durationInSeconds = Duration.between(eventX.time, eventY.time).toMillis() / 1000;
                        long seconds = durationInSeconds % 60;
                        long minutes = (durationInSeconds / 60) % 60;
                        long hours = (durationInSeconds / 60) / 60;

                        if (seconds > 0) {
                            String rowDataAppName = appName.equals("Android System") ? "Screen Locked" : appName;
                            RowData rowData = new RowData(
                                    rowDataAppName, eventX.appIcon, eventX.time, hours + "h " + minutes + "m " + seconds + "s"
                            );
                            rowData.durationInSeconds = durationInSeconds;
                            verboseRowDataList.add(rowData);
                            x = y;
                        }
                    }
                }
            }
        }

        Collections.sort(verboseRowDataList);
        Collections.reverse(verboseRowDataList);
        Collections.reverse(rawRowDataList); //rawRowDataList is already in order
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateAdapter(ArrayList<RowData> rowDataList) {
        listAdapter.setRowDataList(rowDataList);
        listAdapter.notifyDataSetChanged();
    }

    private void highlightButton(Button button) {
        int btnPrimaryColor = getColor(com.google.android.material.R.color.design_default_color_primary);
        int btnOnColor = getColor(com.google.android.material.R.color.design_default_color_on_primary);

        for (Button b : Arrays.asList(btnVerbose, btnConcise, btnRaw)) {
            b.setBackgroundColor(b.equals(button) ? btnPrimaryColor : btnOnColor);
            b.setTextColor(b.equals(button) ? btnOnColor : btnPrimaryColor);
        }
    }

    private boolean isResumedOrNonInteractive(CustomEvent event) {
        return event.eventType.equals("Activity Resumed") || event.eventType.equals("Screen Non-Interactive");
    }

    private boolean isStoppedOrKeyguardHidden(CustomEvent event) {
        return event.eventType.equals("Activity Stopped") || event.eventType.equals("Keyguard Hidden");
    }
}

