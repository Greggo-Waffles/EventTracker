/*
*Name: Greg Pittman
*File: MainActivity.java
*Description: Activity for the login page
*Next Activity/s: MainActivityList.java
*/

package com.greg.eventTracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {
    //Screen element variables
    EditText username;
    EditText password;
    Button Login;
    Button NewUser;

    //Database handler
    private UserPass upHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        upHandler = new UserPass(this);
        upHandler.runDB();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        Login = findViewById(R.id.Login);
        NewUser = findViewById(R.id.NewUser);

        //Login button functionality
        // should check the username and password against the database
        // if correct, should take the user to the main activity
        // if incorrect, should display an error message
        // if no account exists, should display an error message
        // passes the user ID to the main activity
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int userID = upHandler.checkUsernamePassword(username.getText().toString(), password.getText().toString());
                if(userID != -1){
                    Toast.makeText(MainActivity.this, userID + " Logging In", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, MainActivityList.class);
                    intent.putExtra("userID", userID);
                    startActivity(intent);
                }
                else{
                    Toast.makeText(MainActivity.this, "Incorrect Username or Password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //New User button functionality
        // should create a new user account
        // if username is created, should display a success message
        // if username already exists, should display an error message
        NewUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(upHandler.addUser(username.getText().toString(), password.getText().toString())){
                    Toast.makeText(MainActivity.this, "Account Created, Please Login", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, "Username Already Exists", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}