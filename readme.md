Particle Photon Garage Controller (Android Version)
===================================================
This is the Android version of the Particle Photon Garage Door Controller app. It allows a user to remotely open a garage door by simply pushing the button in the app's main screen. 

The app's security is controlled through variables defined in a separate configuration file (Config.java):

	package com.johnwargo.garagedoor;
	
	public class Config {
	    /*******************************************************************************************
	     * The following constants must be populated with the values from your Particle account
	     * and Wi-Fi network.
	     *******************************************************************************************/
	    //  Your Particle.io Access Token
	    public static final String ACCESS_TOKEN = "ACCESS_TOKEN_FROM_THE_PARTICLE_CLOUD";
	    // The Particle Spark, Photon, or Electron Device ID        
	    public static final String DEVICE_ID = "DEVICE_ID_FOR_THE_PHOTON_DEVICE";
	
	    // Change this for your network's SSID
	    // For some reason Android puts the SSID in quotes, so we have to have them here
	    public static final String LOCAL_SSID = "\"SSID_VALUE\"";
	
	    // The array of mobile phone numbers that are used to define whether Override Mode is enabled
	    // Add any phone numbers to this array; if they're in this list, then they get the menu
	    // and other capabilities. Start with country code (1) and don't include any spaces, dashes,
	    // or any other stuff in the phone number.
	    public static String[] OVERRIDE_PHONES = {"13305355555", "13305355556"};
	
	    // The URL for the hosted web app that the single use code is accessed through.
	    // Do NOT put an ending forward slash (/) at the end of the URL
	    public static final String WEB_APP_URI = "http://www.yourdomain.com/garage_door/index.html";
	}


`ACCESS_TOKEN` value is a private code used to provide access to your Particle Cloud account. You can obtain this value (and replace the value in the config file above) from your account on the [Particle Cloud](http://particle.io).

`DEVICE_ID` is the unique identifier for the Particle Photon device you're using for your Garage Controller. You can obtain this ID from the Particle app or from your list of devices listed on the [Particle Cloud](http://particle.io).

The app has a security feature where, by default, app users cannot open the garage door using the app unless they're connected to the home's Wi-Fi network. To identify this network to the app, you have to provide the network's SSID in the `LOCAL_SSID` field in the configuration file. The Android Wi-Fi APIs return the SSID in quotes, so to make this work correctly in the app, you have to provide the quotes as well. That's why a SSID of myssid would be listed in the file as "\"myssid\"", the backslash `\` escapes the following double quote characters.

As a security feature, the app will let certain smartphones open the garage door regardless of whether they're connected to the home's Wi-Fi network. To identify the devices that are exempt from this restriction, add the device(s) phone numbers to the `OVERRIDE_PHONES` array in the configuration file.

The app has a feature that allows it to generate a single-use code and set it in the Particle Photon Garage Door Controller. Once the code has been generated and sent to the controller, a URL that a neighbor, friend or service worker can use to open the garage door is built using the value in the `WEB_APP_URI`field and copied to the device clipboard so you can send the URL using email or SMS. The single use code leverages the web_app_with_code web application project included in this repository. You'll need to publish that app to a web server somewhere, update it's configuration then provide the root app URL here in the `WEB_APP_URI` field. 

Questions? Use the contact form on my [personal blog](http://www.johnwargo.com).

***
You can find information on many different topics on my [personal blog](http://www.johnwargo.com). Learn about all of my publications at [John Wargo Books](http://www.johnwargobooks.com). 
