/*
 *Name: Greg Pittman
 *File: appSettings.java
 *Description: Activity for app settings
 *  Current Settings: SMS Confirmation
 *Next Activity/s: MainActivityList.java
 */
package com.greg.eventTracker;

//Imports
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import android.widget.Toast;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/*
*
 */
public class appSettings extends AppCompatActivity {
    //Screen element variables
    ImageButton exitSettings;
    TextView settingsText;
    Button pushConfirm;
    UserPass UserPass;
    Button pushForce;
    EditText groupCode;
    Button enterCodeButton;



    //Permissions
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1443;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        exitSettings = findViewById(R.id.exitSettings);
        settingsText = findViewById(R.id.settingsText);
        pushConfirm = findViewById(R.id.pushConfirm);
        pushForce = findViewById(R.id.pushForce);
        groupCode = findViewById(R.id.groupCode);
        enterCodeButton = findViewById(R.id.enterCodeButton);
        UserPass = new UserPass(this);
        int uID = getIntent().getIntExtra("userID", -1);

        //Exit button functionality
        // should take the user back to the main activity
        // passes the user ID to the main activity
        exitSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(appSettings.this, MainActivityList.class);
                intent.putExtra("userID", uID);
                startActivity(intent);
            }
        });

        //Push Notification confirmation button functionality
        // should promt the user for permission to send Push Notification confirmation
        // Android permissions window should appear on first click
        // Have not tested functionality if user has already declined permission
        pushConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Check if the permission has already been granted.
                    if (ContextCompat.checkSelfPermission(appSettings.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(appSettings.this, "Notification permission is already enabled.", Toast.LENGTH_SHORT).show();
                    } else {
                        ActivityCompat.requestPermissions(appSettings.this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATION_PERMISSION_REQUEST_CODE);
                    }
                } else {
                    Toast.makeText(appSettings.this, "Notifications are enabled by default on this Android version.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Push Notification force button functionality
        // should send a push notification to the user
        pushForce.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Data inputData = new Data.Builder().putInt("USER_ID", uID).build();
                OneTimeWorkRequest forceRequest = new OneTimeWorkRequest.Builder(NotifWorker.class )
                        .setInputData(inputData).
                        setInitialDelay(5, TimeUnit.SECONDS).build();
                WorkManager.getInstance(appSettings.this).enqueueUniqueWork("TestEventNotification", // A unique name for the test work
                        ExistingWorkPolicy.REPLACE,
                        forceRequest
                );

            }
        });

        //Group Code button functionality
        // should add the group code to the user's list of group codes
        // re-entering the code will remove it from the list
        enterCodeButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                String code = groupCode.getText().toString();
                if(!code.isEmpty()){
                    UserPass.addGroupCode(uID, code);
                }
                else{
                    Toast.makeText(appSettings.this, "Please enter a group code", Toast.LENGTH_SHORT).show();
                }
            }});
        }
    }