package mag.linux.android_sensors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;



/**
 * @author fredericamps@gmail.com - 2017
 *
 * MAPs
 *
 */
public class GeoPointMap extends AppCompatActivity implements OnMapReadyCallback {

    final String TAG="GEOMAP";

    // Google map
    private GoogleMap mMap;

    // List of geocache
    private ArrayList<String> tagListName = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter = null;

    // UI
    ListView lv;

    // Geocache points
    int geoSelected;
    GeoPoint [] geoPointTab=null;
    GeoPoint [] geoPointTabOther=null;
    GeoPoint [] tabGeo=null;

    // sectioned geocache
    LatLng lastGeo;
    String geoTitle;
    String geoName;
    String geoInfo;
    DataManager storageGeo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        computeGeoPoint();

        setContentView(R.layout.activity_geo_point_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        arrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, tagListName);

        lv = (ListView) findViewById(R.id.listViewTagList);
        lv.setAdapter(arrayAdapter);

        arrayAdapter.notifyDataSetChanged();

        // select a geocache point
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setSelected(true);
                geoSelected=position;

                String name = tagListName.get(geoSelected);

                if(!name.trim().isEmpty())
                addGeoMarker(tabGeo[position]);
            }
        });

        // listView long click show geocache info
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long id) {
                view.setSelected(true);

                geoSelected=position;
                String name = tagListName.get(geoSelected);

                if(!name.trim().isEmpty()) {
                Dialog myTextDialog= myTextDialog(tabGeo[position]);
                myTextDialog.show();
                }

                return true;
            }
        });

        mapFragment.getMapAsync(this);
    }




    /**
     *  Get geocache from Data.xml and Other.xml
     */
    void computeGeoPoint()
    {
        int sizeGeoPointList=0;

        int ind=0;

        storageGeo = MainActivity.getDataManager();

        // my geocache
        geoPointTab = storageGeo.getContent(DataManager.xmlFile);

        // other geocache
        geoPointTabOther = storageGeo.getContent(DataManager.xmlFileOther);

        if(geoPointTab!=null && geoPointTab.length>0)
        {
            sizeGeoPointList += geoPointTab.length;
        }

        if(geoPointTabOther!=null && geoPointTabOther.length>0)
        {
            sizeGeoPointList += geoPointTabOther.length;
        }

        if(sizeGeoPointList >0)
        {
            tabGeo =  new GeoPoint[sizeGeoPointList];

            if(geoPointTab!=null && geoPointTab.length>0)
            {
                for(int i=0; i<geoPointTab.length;i++) {
                    ind++;
                    tabGeo[i]=geoPointTab[i];
                }
            }

            if(geoPointTabOther!=null && geoPointTabOther.length>0)
            {
                for(int i=0; i<geoPointTabOther.length;i++) {
                    tabGeo[ind]=geoPointTabOther[i];
                    ind++;
                }
            }
        }


        if(tabGeo != null && tabGeo.length>0)
        {
            for(int i=0; i<tabGeo.length; i++) {
                ind++;
                tagListName.add(tabGeo[i].toString());
            }

            lastGeo = new LatLng(tabGeo[0].getLatitude(), tabGeo[0].getLongitude());
            geoInfo = tabGeo[0].getInfo();
            geoName = tabGeo[0].getName();
        }else{ // default point
            tagListName.add(" ");
            lastGeo = new LatLng(48.866667, 2.333333);
            geoTitle = "Paris";
        }
    }



    void removeElement()
    {
        int ind=0;
        String name;
        GeoPoint [] tmpGeoPt=null;

        if(tabGeo==null || tabGeo.length==0) return;

        name = tagListName.get(geoSelected);

        if(name.trim().isEmpty()) return;

        // delete current selection
        tabGeo[geoSelected] = null;

        // clear vector
        tagListName.clear();

        // delete geoCache in xml file
        storageGeo.delGeoPoint(name, DataManager.xmlFile);
        storageGeo.delGeoPoint(name, DataManager.xmlFileOther);

        // content any geoPoint
        if(tabGeo.length -1 > 0) {
            tmpGeoPt = new GeoPoint[tabGeo.length - 1];

            for (int i = 0; i < tabGeo.length; i++) {

                if (tabGeo[i] != null) {
                    tmpGeoPt[ind++] = tabGeo[i];
                }
            }

            for (int i = 0; i < tmpGeoPt.length; i++)
                tagListName.add(tmpGeoPt[i].toString());

            tabGeo = tmpGeoPt.clone();
            tmpGeoPt = null;

            // get geopoint
            lastGeo = new LatLng(tabGeo[0].getLatitude(), tabGeo[0].getLongitude());
            geoInfo = tabGeo[0].getInfo();
            geoName = tabGeo[0].getName();
            addGeoMarker(tabGeo[0]);
        }
        else if(tabGeo.length - 1 == 0) // no more geoPoint
        {
            tabGeo=null;
            lastGeo=null;
        }

        // notify change data
        arrayAdapter.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //ajoute les entrées de menu_test à l'ActionBar
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }


    //gère le click sur une action de l'ActionBar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){

            case R.id.action_delete:
                removeElement();
                return true;

            case R.id.action_share:
                sendSMS();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //http://stackoverflow.com/questions/8578689/sending-text-messages-programmatically-in-android

    private void sendSMS()
    {
       GeoPoint geoPoint = tabGeo[geoSelected];

        String msg = "geocache: " + geoPoint.getName() + " " + String.valueOf(geoPoint.getLatitude()) + " " + String.valueOf(geoPoint.getLongitude());

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.addCategory(Intent.CATEGORY_DEFAULT);
        smsIntent.setType("vnd.android-dir/mms-sms");
        smsIntent.putExtra("sms_body",msg);
        smsIntent.setData(Uri.parse("sms:"));

        startActivity(smsIntent);
    }


    void addGeoMarker(GeoPoint mGeoCache)
    {
        LatLng geoCache = new LatLng(mGeoCache.getLatitude(), mGeoCache.getLongitude());

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(geoCache).title(mGeoCache.getName()));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(geoCache));
    }


    /**
     * Manipulates the map once available.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Add a marker and move the camera
       mMap.addMarker(new MarkerOptions().position(lastGeo).title(geoTitle));
       mMap.moveCamera(CameraUpdateFactory.newLatLng(lastGeo));
    }


    private Dialog myTextDialog(GeoPoint mGeoPoint) {
        final View layout = View.inflate(this, R.layout.dialog_geo_info, null);

        TextView textViewName = (TextView) layout.findViewById(R.id.textDiagViewName);
        TextView textViewLat = (TextView) layout.findViewById(R.id.textDiagViewLat);
        TextView textViewLng = (TextView) layout.findViewById(R.id.textDiagViewLng);
        TextView textViewInfo = (TextView) layout.findViewById(R.id.textDiagViewInfo);

        textViewName.setText(mGeoPoint.getName());
        textViewLat.setText(String.valueOf(mGeoPoint.getLatitude()));
        textViewLng.setText(String.valueOf(mGeoPoint.getLongitude()));
        textViewInfo.setText(mGeoPoint.getInfo());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);

        builder.setPositiveButton("OK", new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.setView(layout);
       return  builder.create();
    }
}
