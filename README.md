# WifiRssi
Application to capture all RSSI values of a given AP in vicinity.

The application is set so that a user can use the getAccessPoint() method to connect to an access point of their choosing.  To scan all access points ignore this method and remove the if statement from the for loop within the Broadcast Receiver.

    if (getAccessPoint(accessPoint.SSID )) {
    }



The Values will be written out to a given database
