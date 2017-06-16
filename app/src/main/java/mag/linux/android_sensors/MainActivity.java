package mag.linux.android_sensors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import mag.linux.android_sensors.util.IabHelper;
import mag.linux.android_sensors.util.IabResult;
import mag.linux.android_sensors.util.Inventory;
import mag.linux.android_sensors.util.Purchase;


/** @author fredericamps@gmail.com - 2017
 *
 *  Geocaching software with:
 *   - MAPS
 *   - NFC reader/writer
 *   - GPS
 *   - Bluetooth P2P communication
 *   - SMS notification
 *   - Ads banner
 *   - Billing App
 *
 *   !! Add your Google Map Key API in file values/google_maps_api.xml !!
 *
 *
 */
public class MainActivity extends Activity implements SensorEventListener {

    final String TAG="sensor";
    SensorManager mSensorManager;

    private Sensor mRotationVector;

    // sensor vecteur rotation
    float[] orientation = new float[3];
    float[] rotateMat = new float[9];

    // NFC
    private PendingIntent mNfcPendingIntent;
    private NfcAdapter mNfcAdapter;
    NfcTagManager mNfcIntentManager;
    Tag tagFromIntent=null;

    //Gui
    private TextView textDirection;
    private TextView textViewMsgFromNFC;
    static private TextView textLat;
    static private TextView textLong;
    private EditText editextNFCMessage;
    private Button buttonWriteNFC;
    private Button buttonSave;
    private Button buttonShowTags;
    private Button buttonP2P;
    private Button buttonPurchase;

    // GPS
    GPSManager mGPSManager=null;
    double lat=0.0, lgt=0.0;

    //Store manager
    static DataManager mStoreManager;

    // AdMob
    AdView mAdView;

    // In app product
    IabHelper mHelper;
    static final String ITEM_SKU_PRODUCT1 = "produit_1";

    // Security permission
    static boolean PERM_ACCESS_COARSE_LOCATION=false;
    static boolean PERM_ACCESS_FINE_LOCATION=false;
    static boolean PERM_SMS=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {

            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // UI
        textDirection = (TextView) findViewById(R.id.textViewLocation);
        editextNFCMessage = (EditText) findViewById(R.id.editTextNFC);
        buttonWriteNFC = (Button) findViewById(R.id.buttonWriteNFC);
        buttonSave = (Button) findViewById(R.id.buttonSave);
        buttonShowTags = (Button) findViewById(R.id.buttonSeeMyTag);
        buttonP2P = (Button) findViewById(R.id.buttonP2P);
        buttonPurchase = (Button) findViewById(R.id.buttonBuy);


        textViewMsgFromNFC = (TextView) findViewById(R.id.textViewMsgFromNFC);
        textLat = (TextView) findViewById(R.id.textViewLat);
        textLong = (TextView) findViewById(R.id.textViewLong);


        // Write NFC
        buttonWriteNFC.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                String str = editextNFCMessage.getText().toString();
                try {

                    if(!NfcTagManager.NFC_DEVICE || !NfcTagManager.NFC_ENABLE) return;

                    if (tagFromIntent != null)
                        mNfcIntentManager.writeTag(tagFromIntent, str);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }
        });


        // Save location
        buttonSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    if (mGPSManager != null) {
                        lat = mGPSManager.getLatitude();
                        lgt = mGPSManager.getLongitude();

                        Dialog myTextDialog = myTextDialog();
                        myTextDialog.show();
                    }
            }
        });


        // Show tag location
        buttonShowTags.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent mapIntent = new Intent(MainActivity.this.getApplicationContext(), mag.linux.android_sensors.GeoPointMap.class);
                startActivity(mapIntent);
            }
        });


        // BT P2P connection
        buttonP2P.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent p2pIntent = new Intent(MainActivity.this.getApplicationContext(), mag.linux.android_sensors.CommManager.class);
                startActivity(p2pIntent);
            }
        });

        buttonPurchase.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    mHelper.launchPurchaseFlow(MainActivity.this, ITEM_SKU_PRODUCT1, 10001,
                            mPurchaseFinishedListener, "mypurchasetoken");
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        });


        // SensorManager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // NFC
        mNfcIntentManager = new NfcTagManager(this.getApplicationContext());
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Store manager
        mStoreManager = new DataManager(this.getApplicationContext());

        // Permission
        checkAndRequestPermissions();

        // GPS manager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            if(PERM_ACCESS_COARSE_LOCATION &&  PERM_ACCESS_FINE_LOCATION)
            {
                mGPSManager = new GPSManager(this, getApplicationContext());
            }
        }
        else
        {
            mGPSManager = new GPSManager(this, getApplicationContext());
        }

        // Start AdMob
        adMob();

        // In app billing example
        billinApp();
    }


    int PERMISSION_REQUEST_CODE;
    void checkPermission() {


        // If Android 6 or later
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

                Log.d("permission", "permission denied to SEND_SMS - requesting it");
                String[] permissions = {android.Manifest.permission.SEND_SMS};

                PERMISSION_REQUEST_CODE = 1;
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }

            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d("permission", "permission denied COARSE_LOCATION - requesting it");
                String[] permissions = {android.Manifest.permission.ACCESS_COARSE_LOCATION};

                PERMISSION_REQUEST_CODE = 2;
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }

            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d("permission", "permission denied FINE_LOCATION - requesting it");
                String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION};

                PERMISSION_REQUEST_CODE = 3;
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }


    private  void checkAndRequestPermissions() {
        String [] permissions=new String[]{
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        };

        int i=1;

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission:permissions) {
            PERMISSION_REQUEST_CODE=i++;
            if (ContextCompat.checkSelfPermission(this,permission )!= PackageManager.PERMISSION_GRANTED){
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
        }
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

       for(int i=0;i<permissions.length;i++) {
           if (permissions[i].equals("android.permission.SEND_SMS")) {

           }
           if (permissions[i].equals("android.permission.ACCESS_COARSE_LOCATION")) {

           }

           if (permissions[i].equals("android.permission.ACCESS_FINE_LOCATION")) {

           }

           Log.d(TAG, "mypermission = " + permissions[i]);
           Log.d(TAG, "myGrant = " + grantResults[i]);
       }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {

            Log.d(TAG, "onActivityResult handled by IabHelper");

            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    /**
     *
     */
    void billinApp()
    {
        String base64EncodedPublicKey = getString(R.string.applicence);

        // compute your public key and store it in base64EncodedPublicKey
        mHelper = new IabHelper(this, base64EncodedPublicKey);


        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh no, there was a problem.
                    Log.d(TAG, "Problem setting up In-app Billing: " + result);
                    return;
                }
                    // Hooray, IAB is fully set up!
                    Log.d(TAG, "In-app Billing is set up OK");

                    mHelper.enableDebugLogging(true, TAG);

                try {
                    mHelper.queryInventoryAsync(mQueryFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Log.d(TAG, "Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }


    IabHelper.QueryInventoryFinishedListener
            mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory)
        {
            if (mHelper == null) return;

            if (result.isFailure()) {
                // handle error
                return;
            }

            if(inventory!=null) {
                String productList = inventory.toString();
                Log.d(TAG, "Item price = " + productList);
            }

            // update the UI
        }
    };


    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                Log.d(TAG, "Error purchasing: " + result);
                return;
            }

            if (purchase.getSku().equals(ITEM_SKU_PRODUCT1))
            {
                Log.d(TAG, "SKU is Ok");
            }

            Log.d(TAG, "Purchase successful.");
        }
    };


    /**
     *
     */
    void adMob()
    {
        //Google Mobile Ads
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-5973967299137670/1731475544");

        mAdView = (AdView) findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder()
                .setGender(AdRequest.GENDER_FEMALE)
                .setBirthday(new GregorianCalendar(1987, 5, 3).getTime())
                .tagForChildDirectedTreatment(true)
                .build();

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.v(TAG, "************ Open AdMob banner ************");
            }
        });

        mAdView.loadAd(adRequest);
    }


    public static void displayGeo(String lat, String lgt) {
        textLat.setText(lat);
        textLong.setText(lgt);
    }

    public static DataManager getDataManager()
    {
        return mStoreManager;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAdView.resume();

        if(mSensorManager!=null) {
            mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_UI);
        }

        // NFC
        if(NfcTagManager.NFC_DEVICE && NfcTagManager.NFC_ENABLE) {
            mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {

        mAdView.pause();
        super.onPause();
        if(mSensorManager!=null) {
            mSensorManager.unregisterListener(this);
        }

        // NFC
        if(NfcTagManager.NFC_DEVICE && NfcTagManager.NFC_ENABLE) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onDestroy()
    {
        mAdView.destroy();

        if (mHelper != null) try {
            mHelper.dispose();
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
        mHelper = null;

        super.onDestroy();
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        synchronized (this) {

            if( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ){
                // compute rotation matrice
                SensorManager.getRotationMatrixFromVector( rotateMat, event.values );
                // compute azimuth in degree
                updateTextDirection(( Math.toDegrees( SensorManager.getOrientation( rotateMat, orientation )[0] ) +360 ) % 360);
            }
        }
    }

    /**
     *
     * @param bearing
     */
    private void updateTextDirection(double bearing) {
        int range = (int) (bearing / (360f / 16f));
        String directionTxt = "";

        if (range == 15 || range == 0)
            directionTxt = "N";
        if (range == 1 || range == 2)
            directionTxt = "N-E";
        if (range == 3 || range == 4)
            directionTxt = "E";
        if (range == 5 || range == 6)
            directionTxt = "S-E";
        if (range == 7 || range == 8)
            directionTxt = "S";
        if (range == 9 || range == 10)
            directionTxt = "S-O";
        if (range == 11 || range == 12)
            directionTxt = "O";
        if (range == 13 || range == 14)
            directionTxt = "N-O";

        textDirection.setText("" + ((int) bearing) + ((char) 176) + " " + directionTxt); // char 176 = degrees char...
    }


    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            if(BuildConfig.DEBUG)
            {
                Log.v(TAG, intent.getAction());
            }

            tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);;

            String msg = mNfcIntentManager.computeNfcIntent(intent);

            if(msg!=null)
            {
                textViewMsgFromNFC.setText(msg);
            }
        }
    }


    /**
     *
     */
    void sensorDetection()
    {
        if(mSensorManager==null) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }

        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        if(deviceSensors!=null && !deviceSensors.isEmpty()) {

            for (Sensor mySensor : deviceSensors) {
                Log.v(TAG, "info: " + mySensor.toString());
            }

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                Log.v(TAG, "info: Accelerometer found !");
            }
            else {
                Log.v(TAG, "info: Accelerometer not found !");
            }
        }
    }


    String geoInfo;
    String geoName;

    private Dialog myTextDialog() {
        final View layout = View.inflate(this, R.layout.diaglocation, null);
        final EditText savedText = ((EditText) layout.findViewById(R.id.editTextGeoInfo));
        final EditText savedGeoName = ((EditText) layout.findViewById(R.id.editTextGeoName));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);

        builder.setPositiveButton("Save", new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                 geoInfo = savedText.getText().toString().trim();
                 geoName = savedGeoName.getText().toString().trim();

                mStoreManager.addGeoPoint(geoName, String.valueOf(lat), String.valueOf(lgt),  geoInfo);
            }
        });

        builder.setNegativeButton("Cancel", new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                geoInfo=null;
                geoName=null;
            }
        });

        builder.setView(layout);
        return builder.create();
    }

}