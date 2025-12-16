/*
 *Name: Greg Pittman
 *File: Events.java
 *Description: Event Database Class
 */

package com.greg.eventTracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//Defines the database for storing events
public class Events extends SQLiteOpenHelper {
    private Context context;
    private static final String DATABASE_NAME = "Events.db";
    private static final int DATABASE_VERSION = 1;
    public Events(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    //Describes the structure of the events table headers
    private static final class EventsTable {
        private static final String TABLE = "Events";
        private static final String EVENT_ID = "event_id";
        private static final String EVENT_NAME = "event_name";
        private static final String EVENT_DESCRIPTION = "event_description";
        private static final String EVENT_LOCATION = "event_location";
        private static final String EVENT_DATE = "event_date";
        private static final String EVENT_TIME = "event_time";
        private static final String USER_ID = "user_id";
        private static final String GROUP_CODE = "group_code";
    }


    // Function creates the DB and describes the field types
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + EventsTable.TABLE + " (" +
                EventsTable.EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EventsTable.EVENT_NAME + " TEXT, " +
                EventsTable.EVENT_DESCRIPTION + " TEXT, " +
                EventsTable.EVENT_LOCATION + " TEXT, " +
                EventsTable.EVENT_DATE + " TEXT, " +
                EventsTable.EVENT_TIME + " TEXT, " +
                EventsTable.USER_ID + " INTEGER,"+
                EventsTable.GROUP_CODE + " TEXT)");
    }
    public void runDB(){
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + EventsTable.TABLE);
        onCreate(db);
    }
    //Builds empty event
    public long addDefaultEvent(int userID){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(EventsTable.EVENT_NAME, "Default Event");
        values.put(EventsTable.EVENT_DESCRIPTION, "Default Event Description");
        values.put(EventsTable.EVENT_LOCATION, "Default Event Location");
        values.put(EventsTable.EVENT_DATE, "1/1/1900");
        values.put(EventsTable.EVENT_TIME, "12:00");
        values.put(EventsTable.USER_ID, userID);
        values.put(EventsTable.GROUP_CODE, "");
        long eID = db.insert(EventsTable.TABLE, null, values);
        db.close();
        return eID;
    }

    //deletes event
    public boolean deleteEvent(long eventID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(EventsTable.TABLE, EventsTable.EVENT_ID + " = ?", new String[]{String.valueOf(eventID)});
        db.close();
        return true;
    }

    //edits existing event
    public boolean editEvent(long eventID, String eventName, String eventDescription, String eventLocation, String eventDate, String eventTime, String groupCode){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(EventsTable.EVENT_NAME, eventName);
        values.put(EventsTable.EVENT_DESCRIPTION, eventDescription);
        values.put(EventsTable.EVENT_LOCATION, eventLocation);
        values.put(EventsTable.EVENT_DATE, eventDate);
        values.put(EventsTable.EVENT_TIME, eventTime);
        values.put(EventsTable.GROUP_CODE, groupCode);
        db.update(EventsTable.TABLE, values, EventsTable.EVENT_ID + " = ?", new String[]{String.valueOf(eventID)});
        db.close();
        return true;
    }

    //returns existing event based on existing event ID
    public EventData getEvent(int eventID){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + EventsTable.TABLE + " WHERE " + EventsTable.EVENT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(eventID)});
        if(cursor.moveToFirst()){
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            String description = cursor.getString(2);
            String location = cursor.getString(3);
            String date = cursor.getString(4);
            String time = cursor.getString(5);
            int userID = cursor.getInt(6);
            String groupCode = cursor.getString(7);
            EventData event = new EventData(id, name, description, location, date, time, userID, groupCode);
            cursor.close();
            db.close();
            return event;
        }else{
            cursor.close();
            db.close();
            return null;
        }
    }

    // Gets all events for specified user. Ordered by date
    public EventData[] getEvents(int userID){
        UserPass userDB = new UserPass(context);
        String[] userGroupCodesArray = userDB.getGroupCodes(userID);
        List<String> userGroupCodes = new ArrayList<>(Arrays.asList(userGroupCodesArray));
        userDB.close();

        SQLiteDatabase db = this.getReadableDatabase();

        // Sets up dynamic query for group codes attached to user
        StringBuilder whereClause = new StringBuilder(EventsTable.USER_ID + " = ?");
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(userID));

        if (!userGroupCodes.isEmpty() && !(userGroupCodes.size() == 1 && userGroupCodes.get(0).isEmpty())) {
            whereClause.append(" OR " + EventsTable.GROUP_CODE + " IN (");
            for (int i = 0; i < userGroupCodes.size(); i++) {
                whereClause.append("?,");
            }
            whereClause.setLength(whereClause.length() - 1);
            whereClause.append(")");
            selectionArgs.addAll(userGroupCodes);
        }

        String query = "SELECT * FROM " + EventsTable.TABLE + " WHERE " + whereClause.toString()
                + " ORDER BY " + EventsTable.EVENT_DATE + ", " + EventsTable.EVENT_TIME;

        String[] argsArray = selectionArgs.toArray(new String[0]);
        Cursor cursor = db.rawQuery(query, argsArray);

        EventData[] events = new EventData[cursor.getCount()];
        int i = 0;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String description = cursor.getString(2);
                String location = cursor.getString(3);
                String date = cursor.getString(4);
                String time = cursor.getString(5);
                int eventOwnerId = cursor.getInt(6);
                String groupCode = cursor.getString(7); // Correct index is 7

                EventData event = new EventData(id, name, description, location, date, time, eventOwnerId, groupCode);
                events[i] = event;
                i++;
                cursor.moveToNext();
            }
        }
        cursor.close();
        db.close();
        return events;
    }

    //Gets all events scheduled for today for specified user
    //Gets all events
    //Filters for today's date
    public EventData[] todaysEvents(int userID){
        EventData[] allEvents = getEvents(userID);

        List<EventData> todaysList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        String todayString = LocalDate.now().format(formatter);

        for (EventData event : allEvents) {
            if (event.getDate().equals(todayString)) {
                todaysList.add(event);
            }
        }

        return todaysList.toArray(new EventData[todaysList.size()]);
    }

    //Gets all events scheduled for tomorrow for specified user AND their groups
    //Gets all events
    //Filters for tomorrows's date
    public EventData[] tomorrowEvents(int userID){
        EventData[] allEvents = getEvents(userID);

        List<EventData> tomorrowsList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        String tomorrowString = LocalDate.now().plusDays(1).format(formatter);

        for (EventData event : allEvents) {
            if (event.getDate().equals(tomorrowString)) {
                tomorrowsList.add(event);
            }
        }

        return tomorrowsList.toArray(new EventData[tomorrowsList.size()]);
    }
}
