/*
 *Name: Greg Pittman
 *File: NewEvent.java
 *Description: Activity for entering new events
 *Next Activity/s: MainActivityList.java
 */
package com.greg.eventTracker;
//Main Imports
import android.widget.Toast;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//Date and Time picker imports
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.DatePicker;
import android.widget.TimePicker;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

//Location API imports
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


public class NewEvent extends AppCompatActivity {
    //Screen element variables
    EditText eNameBox;
    EditText eDesBox;
    EditText eLocBox;
    EditText eDateBox;
    EditText eTimeBox;
    TextView neUIDbox;
    TextView eIDNumText;
    TextView eCode;
    ImageButton backButton;
    ImageButton deleteButton;
    Button saveButton;
    Intent intent;
    Events events;

    private Calendar selectedDateTime = Calendar.getInstance();
    private Place eventPlace;
    private ActivityResultLauncher<Intent> startAutocomplete;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        eNameBox = findViewById(R.id.eNameBox);
        eDesBox = findViewById(R.id.eDesBox);
        eLocBox = findViewById(R.id.eLocBox);
        eDateBox = findViewById(R.id.eDateBox);
        eTimeBox = findViewById(R.id.eTimeBox);
        backButton = findViewById(R.id.backButton);
        deleteButton = findViewById(R.id.deleteButton);
        saveButton = findViewById(R.id.saveEventButton);
        neUIDbox = findViewById(R.id.neUIDbox);
        eIDNumText = findViewById(R.id.eIDNumText);
        eCode = findViewById(R.id.eCode);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.PLACES_API_KEY);
        }

        startAutocomplete = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            Place place = Autocomplete.getPlaceFromIntent(intent);
                            eventPlace = place;
                            eLocBox.setText(place.getAddress());
                            Log.i("NewEvent", "Place selected: " + place.getName() + ", Address: " + place.getAddress());
                        }
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Log.i("NewEvent", "User canceled autocomplete");
                    }
                });


        //Stops user from using keyboard in time and date fields
        eDateBox.setFocusable(false);
        eDateBox.setClickable(true);
        eTimeBox.setFocusable(false);
        eTimeBox.setClickable(true);
        eLocBox.setFocusable(false);
        eLocBox.setClickable(true);
        eLocBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAutocomplete();
            }
        });

        //fills in fields with existing event information
        //if user clicked on a card to edit an event
        //the fields will be filled with the event information
        Intent intent = getIntent();
        int userID = intent.getIntExtra("userID", -1);
        long eID = intent.getLongExtra("eventID", -1);
        if(intent.hasExtra("eventName")){
            String name = intent.getStringExtra("eventName");
            if(!name.equals(null)){
                eNameBox.setText(name);
            }
        }
        if(intent.hasExtra("eventDescription")){
            String description = intent.getStringExtra("eventDescription");
            if(!description.equals(null)){
                eDesBox.setText(description);
            }
        }
        if(intent.hasExtra("eventLocation")){
            String location = intent.getStringExtra("eventLocation");
            if(!location.equals(null)){
                eLocBox.setText(location);
            }
        }
        if(intent.hasExtra("eventDate")){
            String date = intent.getStringExtra("eventDate");
            if(!date.equals(null)){
                eDateBox.setText(date);
            }
        }
        if(intent.hasExtra("eventTime")){
            String time = intent.getStringExtra("eventTime");
            if(!time.equals(null)){
                eTimeBox.setText(time);
            }
        }

        eIDNumText.setText(String.valueOf(eID));

        String sID = String.valueOf(userID);
        events = new Events(this);
        //Toast.makeText(NewEvent.this, "New Event for UID" + sID, Toast.LENGTH_SHORT).show();
        neUIDbox.setText(sID);

        //Defines functionality for Date box
        //Opens date picker dialog box
        eDateBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = selectedDateTime.get(Calendar.YEAR);
                int month = selectedDateTime.get(Calendar.MONTH);
                int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        NewEvent.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth){
                                selectedDateTime.set(Calendar.YEAR, year);
                                selectedDateTime.set(Calendar.MONTH, month);
                                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

                                String formattedDate = dateFormat.format(selectedDateTime.getTime());
                                eDateBox.setText(formattedDate);
                            }
                        },
                        year, month, day
                );
                datePickerDialog.show();
            }
        });

        //Defines functionality for time box
        //Opens time picker dialog box
        eTimeBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
                int minute = selectedDateTime.get(Calendar.MINUTE);
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        NewEvent.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute){
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDateTime.set(Calendar.MINUTE, minute);
                                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                String formattedTime = timeFormat.format(selectedDateTime.getTime());
                                eTimeBox.setText(formattedTime);
                            }
                        },
                        hour, minute, false);
                timePickerDialog.show();
            }
        });

        //Defines functionality for back button
        // should take the user back to the main activity
        // passes the user ID to the main activity
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewEvent.this, MainActivityList.class);
                intent.putExtra("userID", userID);
                String name = eNameBox.getText().toString();
                if(name.equals("")||name.equals(null)){
                    events.deleteEvent(eID);
                }
                startActivity(intent);
                finish();
            }
        });
        //Defines functionality for save button
        // should save the event
        // passes the user ID to the main activity
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int userID = intent.getIntExtra("userID", -1);
                String name = eNameBox.getText().toString();
                String description = eDesBox.getText().toString();
                String location = eLocBox.getText().toString();
                String date = eDateBox.getText().toString();
                String time = eTimeBox.getText().toString();
                String groupCode = eCode.getText().toString();
                if(name.isEmpty()){
                    Toast.makeText(NewEvent.this, "Please enter a name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(location.isEmpty()){
                    Toast.makeText(NewEvent.this, "Please enter a location", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(date.isEmpty()){
                    Toast.makeText(NewEvent.this, "Please enter a date", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(groupCode.contains(",")){
                    Toast.makeText(NewEvent.this, "Please enter a valid group code", Toast.LENGTH_SHORT).show();
                    return;
                }
                events.editEvent(eID, name, description, location, date, time, groupCode);
                //Toast.makeText(NewEvent.this, "Event " + eID + " Created", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NewEvent.this, MainActivityList.class);
                intent.putExtra("userID", userID);
                startActivity(intent);
                finish();
            }

        });


        //Defines functionality for delete button
        // should delete the event
        // passes the user ID to the main activity
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewEvent.this, MainActivityList.class);
                intent.putExtra("userID", userID);
                events.deleteEvent(eID);
                startActivity(intent);
                finish();
            }
        });

    }

    //Opens autocomplete activity
    private void launchAutocomplete() {
        // Define a list of the fields you want to receive
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

        // Start the autocomplete intent
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountries(Arrays.asList("US"))
                .build(this);

        startAutocomplete.launch(intent);
    }

}