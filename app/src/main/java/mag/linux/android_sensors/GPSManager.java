package mag.linux.android_sensors;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;



/**
 * @author fredericamps@gmail.com - 2017
 *
 * GPS Manager
 *
 */
public class GPSManager implements LocationListener {

    public static final String TAG="TAG_GPS";

    private LocationManager locationManager;
    private Location currentLocation;
    private double bearing = 0;

    private static final int LOCATION_MIN_TIME = 30 * 1000;
    // location min distance
    private static final int LOCATION_MIN_DISTANCE = 15;
    public static final String FIXED = "FIXED";

    MainActivity main;
    double lat;
    double lgt;
    Context mContext;


    GPSManager(MainActivity myMain, Context myContext) {
        mContext = myContext;
        main = myMain;

        // location manager from system service
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            // Call your Alert message
            Toast.makeText(mContext, R.string.GPS_disabled, Toast.LENGTH_LONG).show();
        }

        try {
            // request location data
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, this);

            // get last known position
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (gpsLocation != null) {
                currentLocation = gpsLocation;
            } else {
                // try with network provider
                Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (networkLocation != null) {
                    currentLocation = networkLocation;
                } else {
                    // Fix a position
                    currentLocation = new Location(FIXED);
                    currentLocation.setAltitude(1);
                    currentLocation.setLatitude(43.77409084);
                    currentLocation.setLongitude(1.43368426);
                }

                // set current location
                onLocationChanged(currentLocation);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lgt;
    }



    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
         lat = location.getLatitude();
         lgt = location.getLongitude();
        main.displayGeo(String.valueOf(lat), String.valueOf(lgt));
        Log.v(TAG, "lat = " + String.valueOf(lat) + "   long = " +  String.valueOf(lgt));
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }


    public boolean isGPSEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

}
