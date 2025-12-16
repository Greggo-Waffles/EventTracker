/*
 *Name: Greg Pittman
 *File: UserPass.java
 *Description: User and password database
 */
package com.greg.eventTracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

//Defines the user and password database
public class UserPass extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "UserPass.db";
    private static final int DATABASE_VERSION = 1;

    public UserPass(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public UserPass(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //Defines the user and password table
    private static final class UserPassTable {
        private static final String TABLE = "UserPass";
        private static final String USER_ID = "user_id";
        private static final String USERNAME = "username";
        private static final String SALT = "Salt";
        private static final String HASH = "Hash";
        private static final String GROUP_CODES = "group_codes";
    }

    // Function creates the DB and describes the field types
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + UserPassTable.TABLE + " (" +
                UserPassTable.USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                UserPassTable.USERNAME + " TEXT, " +
                UserPassTable.SALT + " BLOB, " +
                UserPassTable.HASH + " BLOB, " + // Add comma
                UserPassTable.GROUP_CODES + " TEXT" + // Add the new column
                ")");
    }

    public void runDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + UserPassTable.TABLE);
        onCreate(db);
    }

    //Checks if username already exists
    public boolean checkUsername(String username) {
        if (username.equals("")) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + UserPassTable.TABLE + " WHERE " + UserPassTable.USERNAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

    //Checks if username and password match
    public int checkUsernamePassword(String username, String password) {

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            int id = -1;
            try{
                String query = "SELECT " + UserPassTable.USER_ID + ", " +
                        UserPassTable.SALT + ", " +
                        UserPassTable.HASH +
                        " FROM " + UserPassTable.TABLE +
                        " WHERE " + UserPassTable.USERNAME + " = ?";
                cursor = db.rawQuery(query, new String[]{username});
                byte[] salt = null;
                byte[] hash = null;
                if (cursor.moveToFirst()) {
                    int saltIndex = cursor.getColumnIndex(UserPassTable.SALT);
                    int hashIndex = cursor.getColumnIndex(UserPassTable.HASH);
                    int idIndex = cursor.getColumnIndex(UserPassTable.USER_ID);
                    salt = cursor.getBlob(saltIndex);
                    hash = cursor.getBlob(hashIndex);
                    id = cursor.getInt(idIndex);
                }
                //generates salt even if there is no match to keep time consistant between the two
                else{
                    salt = genSalt();
                    hash = new byte[64];
                }
                byte[] hashCheck = hashPassword(password, salt);
                if (!Arrays.equals(hash, hashCheck)) {
                    // If the hashes do not match, Reset the ID to -1.
                    id = -1;
                }
            }
            catch (NoSuchAlgorithmException | InvalidKeySpecException e){
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                e.printStackTrace();
                id = -1;
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
        return id;
    }

    //Adds new user to database
    public boolean addUser(String username, String password) {
        if (username.equals("") || password.equals("")) {
            return false;
        }

        if (!checkUsername(username)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            try {
                byte[] salt = genSalt();
                byte[] hash = hashPassword(password, salt);
                values.put(UserPassTable.USERNAME, username);
                values.put(UserPassTable.SALT, salt);
                values.put(UserPassTable.HASH, hash);
                values.put(UserPassTable.GROUP_CODES, "");
                db.insert(UserPassTable.TABLE, null, values);
                db.close();
            }
            catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }

    // generates random salt
    public static byte[] genSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    //Hashes password
    public static byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        int iterations = 10000; // The number of iterations makes the hash slower
        int keyLength = 512; // Desired key length
        char[] passwordChars = password.toCharArray();

        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, keyLength);
        return skf.generateSecret(spec).getEncoded();
    }

    public String[] getGroupCodes(int userID){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + UserPassTable.GROUP_CODES + " FROM " + UserPassTable.TABLE + " WHERE " + UserPassTable.USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userID)});
        try {
            if(cursor.moveToFirst()) {
                String groupCodes = cursor.getString(0);
                if (groupCodes != null && !groupCodes.isEmpty()) {
                    return groupCodes.split(",");
                }
            }
        } finally {
            cursor.close();
            db.close();
        }
        return new String[0];
    }

    public boolean addGroupCode(int userID, String groupCode){
        if(groupCode.equals("")||groupCode.equals(",")){
            return false;
        }
        String[] groupCodes = getGroupCodes(userID);
        ArrayList<String> groupCodesList = new ArrayList<String>(Arrays.asList(groupCodes));
        if (groupCodesList.size() == 1 && groupCodesList.get(0).isEmpty()) {
            groupCodesList.clear();
        }
        if(!groupCodesList.contains(groupCode)) {
            groupCodesList.add(groupCode);
        }
        else if(groupCodesList.contains(groupCode)){
            groupCodesList.remove(groupCode);
        }
        String updatedGroupCodes = String.join(",",groupCodesList);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserPassTable.GROUP_CODES, updatedGroupCodes);
        db.update(UserPassTable.TABLE, values, UserPassTable.USER_ID  + " = ?", new String[]{String.valueOf(userID)});
        db.close();
        return true;


    }
}
