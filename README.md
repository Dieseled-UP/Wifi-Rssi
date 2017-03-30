# WifiRssi
Application to capture all RSSI values of a given AP in vicinity.

The application is set so that a you can use the getAccessPoint() method to connect to an access point of your choosing.

    public boolean getAccessPoint(String ssid) {

        return ssid.equalsIgnoreCase("AccessPoint");

    }
Change the name "AccessPoint" to the name of the access point that you wish to scan.
To scan all access points ignore this method and remove the if statement from the for loop within the Broadcast Receiver.

    if (getAccessPoint(accessPoint.SSID )) {
    }


The Values will be written out to a given database
I have used Volley to write out the data, you just need to change the url to your own destination.
    
    private static final String REGISTER_URL = "http://pathTo/yourUploadFile.php";

