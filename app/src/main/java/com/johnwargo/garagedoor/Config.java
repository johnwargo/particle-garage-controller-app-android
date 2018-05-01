package com.johnwargo.garagedoor;

public class Config {
    /*******************************************************************************************
     * The following constants must be populated with the values from your Particle account
     * and Wi-Fi network.
     *******************************************************************************************/
    //  Your Particle.io Access Token
    public static final String ACCESS_TOKEN = "cb65e54867719e5f1ee06e5daa2106e0bc9b74f5";
    // The Particle Spark, Photon, or Electron Device ID
    // Development device
    //public static final String DEVICE_ID = "33002e000947353138383138";
    //Production device
    public static final String DEVICE_ID = "2e002a000447343339373536";


    // Change this for your network's SSID
    // For some reason Android puts the SSID in quotes, so we have to have them here
    public static final String LOCAL_SSID = "\"Wargo\"";

    // The array of mobile phone numbers that are used to define whether Override Mode is enabled
    // Add any phone numbers to this array; if they're in this list, then they get the menu
    // and other capabilities. Start with country code (1) and don't include any spaces, dashes,
    // or any other stuff in the phone number.
    public static String[] OVERRIDE_PHONES = {"17044089758", "17048041244"};

    // The URL for the hosted web app that the single use code is accessed through.
    // Do NOT put an ending forward slash (/) at the end of the URL
    public static final String WEB_APP_URI = "http://www.johnwargo.com/garage_door/index.html";
}
