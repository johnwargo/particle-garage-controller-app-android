package com.johnwargo.garagedoor;

//todo: set the action bar subtitle

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity
        extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String PARTICLE_CLOUD_ROOT = "https://api.particle.io/v1/devices/";
    private static final String URL_PUSH = PARTICLE_CLOUD_ROOT + Config.DEVICE_ID + "/pushButton";
    private static final String URL_SET_CODE = PARTICLE_CLOUD_ROOT + Config.DEVICE_ID + "/setCode";
    private static final int REQUEST_SMS = 0;
    private static final String TAG = "Garage Door Opener";

    private Context context;
    private Button mBtnPush;
    private OkHttpClient client;
    private ProgressBar spinner;
    //Used to store whether the app is in override mode (doesn't check the Wi-Fi SSID)
    boolean mIsOverride;
    private String msgText;
    private String mPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean mIsLocalWiFi;

        super.onCreate(savedInstanceState);

        //Get the current context
        context = getApplicationContext();
        //Setup the button
        setContentView(R.layout.activity_main);

        //Get a reference to the Push button
        mBtnPush = (Button) findViewById(R.id.btnPush);
        //Now define the click listener for the button
        mBtnPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Change the button text to show we're doing something
                setButtonStatus(R.string.altButtonText);
                //Call the Particle service
                pushButton(mPhoneNumber);
            }
        });

        //Setup the progress bar
        spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);

        // Setup OkHTTP https://guides.codepath.com/android/Using-OkHttp
        //Set our HTTP client timeout parameters
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        /*****************************************************************************************
         * Try to get the device phone number so we can check to see it it's one of the override
         * phone numbers. Permissions code taken from https://goo.gl/j4QcEC.
         ****************************************************************************************/
        //Do we have permission to get the phone number
        Log.i(TAG, "Getting device phone number...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            //No? THen we'll have to ask the user before we can do anything
            Log.i(TAG, "Requesting access to the device phone number.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS},
                    REQUEST_SMS);
        } else {
            //We have permission, so grab the phone number and figure out whether the device
            //is in the override number list
            mIsOverride = getOverrideMode();
        }

        /*****************************************************************************************
         * Now we need to see if the device is on the local Wi-Fi network. If it is, then all
         * features are enabled. If not, then we'll only enable app features if override mode
         * is enabled.
         ****************************************************************************************/
        if (!mIsOverride) {
            //Only check the Wi-Fi if we're NOT in override mode
            Log.i(TAG, "Checking network configuration...");
            mIsLocalWiFi = checkNetworkName();
            //Are we not on a network?
            if (!mIsLocalWiFi) {
                //Tell the user that we're in trouble
                msgText = "Device is not connected to the " + Config.LOCAL_SSID + " Wi-Fi network.";
                Log.e(TAG, msgText);
                showErrorDialog(msgText);
                //Now, if we're not in override mode
                if (!mIsOverride) {
                    //Disable the button and change the button text so the app won't work
                    setButtonStatus(R.string.noNetButtonText);
                    //Otherwise, it doesn't matter which network we're on, it will work from anywhere
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        //Only display the menu if the app is in Override mode
        if (mIsOverride) {
            //First add the click listener for the menu item
            MenuItem mItem = menu.findItem(R.id.menu_item_one_time_code);
            mItem.setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Log.i(TAG, "Entering onMenuItemClick()");

                            //Create new single use code
                            UUID mCode = UUID.randomUUID();
                            Log.i(TAG, "Single use code: " + mCode.toString());

                            // Build a url the user can use to open the garage door
                            // we want to send it to someone so they can use it
                            // This URL works from ANYWHERE, so be careful passing it around.
                            // Also, it's a single-use code, so only works once.
                            final String openURL = Config.WEB_APP_URI + "?uuid=" + mCode.toString();
                            Log.i(TAG, "Single use URL: " + openURL);

                            //Send it to the Particle Cloud
                            Log.i(TAG, "Registering single use code with the Photon");
                            try {
                                //Create the HTTP request body and populate it
                                Log.i(TAG, "Building request");
                                RequestBody body = new FormBody.Builder()
                                        .add("access_token", Config.ACCESS_TOKEN)
                                        .add("params", mCode.toString())
                                        .build();
                                //Set our progress spinner
                                spinner.setVisibility(View.VISIBLE);
                                //Call the Particle service
                                Request request = new Request.Builder()
                                        //.url(url)
                                        .url(URL_SET_CODE)
                                        .post(body)
                                        .build();
                                Log.i(TAG, "Executing request");
                                client.newCall(request).enqueue(new Callback() {

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        Log.i(TAG, "Entering onResponse()");

                                        Headers responseHeaders = response.headers();
                                        for (int i = 0; i < responseHeaders.size(); i++) {
                                            Log.i(TAG, "Header: " + responseHeaders.name(i) + ": " + responseHeaders.value(i));
                                        }
                                        Log.i(TAG, "Response body: " + response.body().string());
                                        Log.i(TAG, "Message: " + response.message());
                                        if (response.isSuccessful()) {
                                            SetCodeRunnable runnable = new SetCodeRunnable(openURL);
                                            runOnUiThread(runnable);
                                        } else {
                                            throw new IOException("Unexpected code " + response);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        Log.i(TAG, "Entering onFailure()");
                                        Log.e(TAG, "Exception: " + e.getMessage());
                                        RunnableDialog runnable = new RunnableDialog(e.getMessage());
                                        runOnUiThread(runnable);
                                    }

                                });

                            } catch (Exception e) {
                                Log.e(TAG, "Exception: " + e.getMessage());
                                RunnableDialog runnable = new RunnableDialog(e.getMessage());
                                runOnUiThread(runnable);
                            }
                            //We're done here, so let the device OS know we dealt with the
                            //menu item
                            return true;
                        }
                    });

            return true;
        }
        return false;
    }

    private boolean checkNetworkName() {
        /*****************************************************************************************
         * Check Wi-Fi connectivity
         * We need to make sure the device is on Wi-Fi and connected to the right network before
         * continuing. Use the ConnectivityManager to get access to connection information
         ****************************************************************************************/
        Log.i(TAG, "Entering checkNetworkName()");
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //Get the active network connection
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            //Do we have network connectivity?
            if (activeNetwork != null) { // connected to the internet
                //Is Wi-Fi enabled?
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    //Now, do we have the right SSID?
                    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.i(TAG, "SSID: " + wifiInfo.getSSID());
                    Log.i(TAG, "MAC address: " + wifiInfo.getMacAddress());
                    Log.i(TAG, "Link speed: " + wifiInfo.getLinkSpeed());
                    //Is it the 'expected' SSID?
                    if (!wifiInfo.getSSID().equals(Config.LOCAL_SSID)) {
                        Log.e(TAG, "This is not the SSID you are looking for (obscure Star Wars reference)");
                        return false;
                    }
                } else {
                    Log.e(TAG, "Device is not connected to a Wi-Fi network");
                    return false;
                }
            } else {
                // not connected to the internet
                Log.e(TAG, "Device is not connected to a network");
                return false;
            }
            //We got this far, so everything must be OK
            return true;
        } catch (Exception e) {
            msgText = "Exception: " + e.getMessage();
            Log.e(TAG, msgText);
            showErrorDialog(msgText);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "Entering onRequestPermissionsResult()");
        if (requestCode == REQUEST_SMS) {
            // Received permission result for READ_SMS permission.
            Log.i(TAG, "Received response for READ_SMS permission request.");
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS permission has been granted
                Log.i(TAG, "READ_SMS permission has been granted by the user.");
                mIsOverride = getOverrideMode();
            } else {
                //oops, user said no to READ_SMS permissions
                Log.i(TAG, "READ_SMS permission denied by the user.");
                //Then we don't enable override mode
                mIsOverride = false;
            }
        } else {
            //must be some other permission request
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean getOverrideMode() {
        Log.i(TAG, "Entering getOverride()");
        //Get the phone number for the device, permissions should all have been resolved by now
        mPhoneNumber = getDevicePhoneNumber();
        //If we don't have a number (should never be null, but have to check
        if (mPhoneNumber == null) {
            // hmmm, that shouldn't happen
            Log.e(TAG, "Device phone number is null");
            //Then we can't enable override mode
            return false;
        }
        //Check to see if the number we got is one of the override numbers
        if (Arrays.asList(Config.OVERRIDE_PHONES).contains(mPhoneNumber)) {
            Log.i(TAG, "Phone number match with OVERRIDE_PHONES");
            //Display a Toast indicating success
            CharSequence text = "Override mode enabled";
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(context, text, duration).show();
            return true;
        }
        //We got here, so must not be a match, move along
        CharSequence text = "Override mode disabled";
        int duration = Toast.LENGTH_SHORT;
        Toast.makeText(context, text, duration).show();
        return false;
    }

    private String getDevicePhoneNumber() {
        Log.i(TAG, "Entering getDevicePhoneNumber()");
        // The app will use the device's phone number to determine whether or not to enable
        // override mode.
        try {
            String mPhoneNumber = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number();
            // Change the country code below to match your source country or use the Telephony API
            // to retrieve it from the device.
            Log.i(TAG, "Device phone number: " + mPhoneNumber);
            return mPhoneNumber;
        } catch (Exception e) {
            msgText = "Exception: " + e.getMessage();
            Log.e(TAG, msgText);
            showErrorDialog(msgText);
        }
        //We got this far, so it must not have worked
        return null;
    }

    private void showErrorDialog(String msgText) {
        Log.i(TAG, "Entering showErrorDialog()");
        //Now tell the user why the app won't work.
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Photon Garage Door Opener");
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setMessage("Network connection error: \n\n" + msgText);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Sorry!",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void setButtonStatus(int btnTextID) {
        Log.i(TAG, "Entering setButtonStatus()");
        //Set the text on the button
        mBtnPush.setText(btnTextID);
        //set disabled status based on which text we're adding
        mBtnPush.setEnabled(btnTextID != R.string.altButtonText);
    }

    private void pushButton(String devicePhone) {
        //todo Add confirmation prompt when override mode and not on local Wi-Fi
        try {
            //Create the HTTP request body and populate it
            Log.i(TAG, "Building request");

            //Get the device phone number in a format you can use, if possible
            String thePhoneNum = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                //format the phone number cleanly (if you can)
                thePhoneNum = PhoneNumberUtils.formatNumber(devicePhone, "US");
            } else {
                //Otherwise, use the plain version of the number
                thePhoneNum = devicePhone;
            }

            RequestBody body = new FormBody.Builder()
                    .add("access_token", Config.ACCESS_TOKEN)
                    .add("params", thePhoneNum)
                    .build();
            //Call the Particle service
            Request request = new Request.Builder()
                    .url(URL_PUSH)
                    .post(body)
                    .build();
            Log.i(TAG, "Executing request");
            //Set our progress spinner
            spinner.setVisibility(View.VISIBLE);
            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i(TAG, "Entering onResponse()");
                    // dump the response data to logcat (just for fun)
                    Headers responseHeaders = response.headers();
                    for (int i = 0; i < responseHeaders.size(); i++) {
                        Log.i(TAG, "Header: " + responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }
                    Log.i(TAG, "Response body: " + response.body().string());
                    Log.i(TAG, "Message: " + response.message());
                    //Now see what we have to tell the user
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Request successful");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //reset the button text
                                setButtonStatus(R.string.defaultButtonText);
                                // Disable the progress spinner
                                spinner.setVisibility(View.GONE);
                                //Display a little toast
                                CharSequence text = "The button was pushed!";
                                int duration = Toast.LENGTH_LONG;
                                Toast.makeText(context, text, duration).show();
                            }
                        });
                    } else {
                        Log.e(TAG, "Request failure");
                        Log.e(TAG, "Message: " + response.message());
                        RunnableDialog runnable = new RunnableDialog(response.message());
                        runOnUiThread(runnable);
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i(TAG, "Entering onFailure()");
                    Log.e(TAG, "Exception: " + e.getMessage());
                    RunnableDialog runnable = new RunnableDialog(e.getMessage());
                    runOnUiThread(runnable);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            RunnableDialog runnable = new RunnableDialog(e.getMessage());
            runOnUiThread(runnable);
        }
    }

    public class SetCodeRunnable implements Runnable {
        private String theURL;

        public SetCodeRunnable(String _theURL) {
            this.theURL = _theURL;
        }

        public void run() {
            // Disable the progress spinner
            spinner.setVisibility(View.GONE);
            //Copy single-use URL to clipboard
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("simple text", this.theURL);
            clipboard.setPrimaryClip(clip);
            //Tell the user that the code URL is on the clipboard
            CharSequence text = "Single use code URL copied to clipboard";
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(context, text, duration).show();
        }
    }

    public class RunnableDialog implements Runnable {
        private String theMessage;

        public RunnableDialog(String _theMessage) {
            this.theMessage = _theMessage;
        }

        public void run() {
            // Disable the progress spinner
            spinner.setVisibility(View.GONE);
            //reset the button text
            setButtonStatus(R.string.defaultButtonText);
            //Display the error dialog
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Photon Garage Door Opener");
            alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
            alertDialog.setMessage("Error: " + this.theMessage);
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }
}
