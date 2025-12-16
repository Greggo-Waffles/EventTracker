/*
 *Name: Greg Pittman
 *File: EventData.java
 *Description: Class for storing event data
 */

package com.greg.eventTracker;

//defines the class for storing event data

public class EventData {
    private int id;
    private String name;
    private String description;
    private String location;
    private String date;
    private String time;
    private String groupCode;
    private int userID;

    // Constructor for the EventData class
    // takes in all event fields as parameters
    // no fields should be null
    public EventData(int id, String name, String description, String location, String date, String time, int userID, String groupCode) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.date = date;
        this.time = time;
        this.userID = userID;
        this.groupCode = groupCode;
    }

    // Getters and setters for all event fields

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

}
