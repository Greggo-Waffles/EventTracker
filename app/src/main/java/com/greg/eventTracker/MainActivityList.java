/*
 *Name: Greg Pittman
 *File: MainActivityList.java
 *Description: Activity for displaying events
 *Next Activity/s: NewEvent.java, appSettings.java
 */
package com.greg.eventTracker;

import android.content.Intent;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;

//Push notification imports
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;


public class MainActivityList extends AppCompatActivity {
    //Screen element variables
    ImageButton newEvent;
    ImageButton appSettings;
    Button buttonSortDate;
    Button buttonSortTitle;
    Button buttonSortLoc;
    RecyclerView eventGrid;
    Events events;
    private EventAdapter adapter;
    private List<EventData> eventDataList = new ArrayList<>();
    private  LocalDateTime now = LocalDateTime.now();
    private UserPass upHandler = new UserPass(this);

    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        newEvent = findViewById(R.id.newEvent);
        appSettings = findViewById(R.id.appSettings);
        eventGrid = findViewById(R.id.eventGrid);
        buttonSortDate = findViewById(R.id.buttonSortDate);
        buttonSortTitle = findViewById(R.id.buttonSortTitle);
        buttonSortLoc = findViewById(R.id.buttonSortLoc);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        eventGrid.setLayoutManager(layoutManager);
        adapter = new EventAdapter(eventDataList);
        eventGrid.setAdapter(adapter);
        events = new Events(this);
        events.runDB();

        Intent uIDIntent = getIntent();
        int userID = uIDIntent.getIntExtra("userID",-1);

        if (userID != -1) {
            loadEvents(userID);
            scheduleDailyNotificationWorker(userID);
        }
        //Defines functionality for app settings button
        // should take the user to the app settings activity
        // passes the user ID to the app settings activity
        appSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityList.this, appSettings.class);
            intent.putExtra("userID", userID);
            startActivity(intent);
        });
        //Defines functionality for new event button
        // should take the user to the new event activity
        // passes the user ID to the new event activity
        newEvent.setOnClickListener(v -> {
            long eID = events.addDefaultEvent(userID);
            Intent intent = new Intent(MainActivityList.this, NewEvent.class);
            intent.putExtra("userID", userID);
            intent.putExtra("eventID", eID);
            startActivity(intent);
        });
        //Defines functionality for sort by date button
        // should sort the event list by date
        buttonSortDate.setOnClickListener(v -> {
            loadEvents(userID);
            adapter.notifyDataSetChanged();
        });

        //Defines functionality for sort by title button
        //
        buttonSortTitle.setOnClickListener(v -> {
            quickSort(eventDataList,0,eventDataList.size()-1);
            adapter.notifyDataSetChanged();
        });

        //Defines functionality for sort by location button
        buttonSortLoc.setOnClickListener(v -> {
            eventDataList.sort((e1, e2) -> e1.getLocation().compareTo(e2.getLocation()));
            adapter.notifyDataSetChanged();
        });
    }
    //Loads events for specified user into the event list
    private void loadEvents(int userID) {
        now = LocalDateTime.now();
        String[] codes = upHandler.getGroupCodes(userID);
        EventData[] eventList = events.getEvents(userID);
        updateEvents(eventList);
    }
    //Updates the event list
    // clears the event list and adds the new events
    private void updateEvents(EventData[] eventList){
        now = LocalDateTime.now();
        eventDataList.clear();
        if (eventList != null && eventList.length > 0) {
            for (EventData event : eventList) {
                if (event != null) {
                    try{
                        String dateTime = event.getDate() + " " + event.getTime();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm a");
                        LocalDateTime eventDateTime = LocalDateTime.parse(dateTime, formatter);

                        if (eventDateTime.isAfter(now)) {
                            eventDataList.add(event);
                        }
                    }
                    catch (Exception e){
                    }
                }
            }
        }
    }

    //Passes the user ID to the worker so it knows whose events to check
    //Create a periodic request to run roughly once a day
    //prevents multiple notifications being sent
    private void scheduleDailyNotificationWorker(int userID) {
        Data inputData = new Data.Builder()
                .putInt("USER_ID", userID)
                .build();

        PeriodicWorkRequest dailyNotificationRequest =
                new PeriodicWorkRequest.Builder(NotifWorker.class, 1, TimeUnit.DAYS)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyEventChecker",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyNotificationRequest
        );
    }

    //Updates the event list when the activity is resumed
    @Override
    protected void onResume() {
        super.onResume();
        Intent uIDIntent = getIntent();
        int userID = uIDIntent.getIntExtra("userID",-1);
        loadEvents(userID);
    }

    //Defines the event holder class
    // describes how the event cards constructed
    private class EventHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView eventName;
        private TextView eventDate;
        private TextView eventTime;
        private TextView eventDesc;
        private TextView eventLoc;
        private EventData event;
        public EventHolder(LayoutInflater inflater, ViewGroup parent){
            super(inflater.inflate(R.layout.cardlayout, parent, false));
            eventName = itemView.findViewById(R.id.cardEventName);
            eventDate = itemView.findViewById(R.id.cardEventDate);
            eventTime = itemView.findViewById(R.id.cardEventTime);
            eventDesc = itemView.findViewById(R.id.cardEventDesc);
            eventLoc = itemView.findViewById(R.id.cardEventLoc);
            itemView.setOnClickListener(this);
        }
        public void bind(EventData event, int position){
            this.event = event;
            eventName.setText(event.getName());
            eventDate.setText(event.getDate());
            eventTime.setText(event.getTime());
            eventDesc.setText(event.getDescription());
            eventLoc.setText(event.getLocation());
        }
        //Defines functionality for event card
        // should take the user to the new event activity
        // passes the user ID, event ID, event name, event description, event location, event date,
        // and event time to the new event activity
        // Functions as edit event
        @Override
        public void onClick(View v) {
            int uID = event.getUserID();
            long eID = event.getId();
            String name = event.getName();
            String description = event.getDescription();
            String location = event.getLocation();
            String date = event.getDate();
            String time = event.getTime();

            Intent intent = new Intent(MainActivityList.this, NewEvent.class);
            intent.putExtra("userID", uID);
            intent.putExtra("eventID", eID);
            intent.putExtra("eventName", name);
            intent.putExtra("eventDescription", description);
            intent.putExtra("eventLocation", location);
            intent.putExtra("eventDate", date);
            intent.putExtra("eventTime", time);
            startActivity(intent);
        }
    }

    //Defines the event adapter class
    //describes how the event cards are in laid out in the recycler view
    private class EventAdapter extends RecyclerView.Adapter<EventHolder>{
        private List<EventData> events;
        public EventAdapter(List<EventData> events){
            this.events = events;
        }
        @NonNull
        @Override
        public EventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivityList.this);
            return new EventHolder(layoutInflater, parent);
        }
        @Override
        public void onBindViewHolder(@NonNull EventHolder holder, int position) {
            EventData event = events.get(position);
            holder.bind(event, position);
        }
        @Override
        public int getItemCount() {
            if(events == null){
                return 0;
            }
            else {
                return events.size();
            }
        }
    }


    //Basic QuickSort algorithm for sorting events by title
    //Could be adapted for any of the fields.
    public void quickSort(List<EventData> eList, int low, int high){
        if(low < high){
            int part = partition(eList, low, high);
            quickSort(eList, low, part-1);
            quickSort(eList, part+1, high);
        }
    }
    public int partition(List<EventData> eList, int low, int high){
        EventData pivot = eList.get(high);
        String pivotName = pivot.getName();

        int i = low - 1;
        for(int j = low; j < high; j++){
            if(eList.get(j).getName().compareTo(pivotName) <= 0) {
                i++;
                EventData temp = eList.get(i);
                eList.set(i, eList.get(j));
                eList.set(j, temp);
            }
        }
        EventData temp = eList.get(i+1);
        eList.set(i+1, eList.get(high));
        eList.set(high, temp);
        return i+1;
    }

}