package mag.linux.android_sensors;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author fredericamps@gmail.com - 2017
 *
 * Bluetooth P2P communication
 *
 */
public class CommManager extends Activity {

    protected static final String TAG = "BLUETOOTH";
    protected static final int DISCOVERY_REQUEST = 1;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice btDeviceSelected;
    UUID MY_UUID = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");

    private ArrayList<String> deviceListName = new ArrayList<String>();
    private ArrayList<BluetoothDevice> deviceBT = new ArrayList<BluetoothDevice>();

    BtServer mBtServer;
    BtClient mBtClient;
    ManageConnectedSocket mManageConnectedSocket;
    ManageConnectedSocket mySocketManager;
    boolean transaction = false;

    // UI
    private ListView lv;
    ArrayAdapter<String> arrayAdapter = null;
    TextView mesgText;
    boolean firstElement = false;

    EditText msg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_p2p);

        //UI
        Button btnStartServer = (Button) findViewById(R.id.buttonStartServer);
        Button btnScan = (Button) findViewById(R.id.buttonScan);
        Button btnDiscover = (Button) findViewById(R.id.buttonDiscoverable);
        Button btnSendData = (Button) findViewById(R.id.buttonSendData);
        Button btnStopServer = (Button) findViewById(R.id.buttonStopServer);

        mesgText = (TextView) findViewById(R.id.textViewMsg);
        lv = (ListView) findViewById(R.id.listViewDevice);


        // data devices
        deviceListName.add(" ");
        arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                deviceListName);

        lv.setAdapter(arrayAdapter);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
        }


        btnStartServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mesgText.setText("Start BT server");

                if (mBtServer == null && mBluetoothAdapter != null) {
                    mBtServer = new BtServer(mBluetoothAdapter, MY_UUID);
                    mBtServer.start();
                }
            }
        });


        btnDiscover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (mBluetoothAdapter.isEnabled()) {
                    makeDiscoverable();
                }
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (!mBluetoothAdapter.isDiscovering() && mBluetoothAdapter.isEnabled()) {
                    // reset list
                    deviceListName.clear();
                    deviceBT.clear();

                    getPairedDevices(mBluetoothAdapter);

                    if (deviceListName.isEmpty()) {
                        deviceListName.add(" ");
                        firstElement = true;
                    }

                    arrayAdapter.notifyDataSetChanged();

                    //start discovery
                    mBluetoothAdapter.startDiscovery();
                    mesgText.setText("Scan in progress ... wait");
                }
            }
        });


        btnSendData.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (!mBluetoothAdapter.isDiscovering()) {

                    if (btDeviceSelected == null) {
                        refreshUI("Select a device before ...");
                    } else if (!transaction) {

                        refreshUI("Send file ...");
                        sendFile();
                    }
                }
            }
        });


        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {

                view.setSelected(true);
                btDeviceSelected = deviceBT.get(position);

                if (btDeviceSelected.getBondState() == BluetoothDevice.BOND_NONE) {

                    mesgText.setText("Pairing... ... wait");
                    pairDevice(btDeviceSelected);
                }
            }
        });


        btnStopServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBtServer != null) {
                    mBtServer.cancel();
                    mBtServer = null;
                    mesgText.setText("Server stopped");
                }

                if (mManageConnectedSocket != null) {
                    mManageConnectedSocket.cancel();
                }
            }
        });


        // algo
        initBluetooth();
        makeDiscoverable();

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);


        // Get paired devices
        //deviceListName.clear();
        deviceBT.clear();
        getPairedDevices(mBluetoothAdapter);
    }


    /**
     *
     */
    private void sendFile() {
        transaction = true;

        refreshUI("Send data in progress ...");

        //client
        if (mBtClient == null) {
            mBtClient = new BtClient(btDeviceSelected, mBluetoothAdapter, MY_UUID);
            mBtClient.start();
        }

        BufferedReader reader;

        try {
            // delay to create BT socket
            while (mySocketManager == null) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            String path = getFilesDir() + "/" + DataManager.xmlFile;

            Log.v(TAG, "**** sending : " + path);

            InputStream is = new FileInputStream(path);
            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();

            // Write to BT socket
            while (line != null) {
                Log.v(TAG, "************ " + line);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mySocketManager.write(line);
                line = reader.readLine();
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        mySocketManager.write("stop");

        // wait BT interface
        try {
            Thread.sleep(1000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        refreshUI("Data transferred ...");


        mBtClient.cancel();
        mBtClient = null;
        mySocketManager = null;
        transaction = false;
    }


    /**
     * @param mBluetoothAdapter
     * @return
     */
    private Set<BluetoothDevice> getPairedDevices(BluetoothAdapter mBluetoothAdapter) {
        Set<android.bluetooth.BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {

            deviceListName.clear();

            // There are paired devices. Get the name and address of each paired device.
            for (android.bluetooth.BluetoothDevice device : pairedDevices) {

                Log.v(TAG, "paired = " + device.getName());
                Log.v(TAG, "paired MAC = " + device.getAddress());

                deviceBT.add(device);
                deviceListName.add(device.getName() + "  paired");

                arrayAdapter.notifyDataSetChanged();
            }
        }
        return pairedDevices;
    }


    /**
     * @param device
     */
    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param device
     */
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                refreshUI("Discovery started");
                Log.v(TAG, "ACTION_DISCOVERY_STARTED");

                //discovery starts, we can show progress dialog or perform other tasks
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
                mesgText.setText("Discovery finished");
                Log.v(TAG, "ACTION_DISCOVERY_FINISHED");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (!deviceListName.isEmpty() && firstElement) {
                    firstElement = false;
                    deviceListName.clear();
                }

                deviceBT.add(device);
                deviceListName.add(device.getName());

                arrayAdapter.notifyDataSetChanged();
                Log.v(TAG, "Found device " + device.getName());

            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.v(TAG, "Paired");

                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    Log.v(TAG, "Unpaired");
                } else {
                    Log.v(TAG, "State bond has changed");
                }
            } else {
                Log.v(TAG, "State changed with : " + action);
            }
        }
    };


    /**
     *
     */
    private static final int ENABLE_BLUETOOTH = 1;

    private void initBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH);
        } else {

        }
    }

    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == ENABLE_BLUETOOTH)
            if (resultCode == RESULT_OK) {

                Log.v(TAG, "BT = " + ENABLE_BLUETOOTH);
            }

        if (requestCode == DISCOVERY_REQUEST) {
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Discovery cancelled by user");
            } else {
                Log.v(TAG, "Discovery allowed");
            }
        }
    }

    /**
     *
     */
    private void makeDiscoverable() {

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

        //discoverable for 5 minutes (~300 seconds)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);

        startActivityForResult(discoverableIntent, DISCOVERY_REQUEST);

        Log.i("Log", "Discoverable ");
    }


    /**
     * Server
     */
    private class BtServer extends Thread {

        BluetoothServerSocket mmServerSocket = null;


        public BtServer(BluetoothAdapter mBTdapter, UUID mUUID) {

            UUID MY_UUID = mUUID;
            BluetoothAdapter mBluetoothAdapter = mBTdapter;

            try {
                Log.v(TAG, "mBluetoothAdapter ...");
                mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("server", MY_UUID);
                Log.v(TAG, "mBluetoothAdapter ok");


            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {

                    Log.v(TAG, "wait cx");

                    // if (mmServerSocket != null) {
                    socket = mmServerSocket.accept();
                    // }

                    Log.v(TAG, "SocketAccepted");

                    // if (socket != null) {
                    Log.v(TAG, "Manage cx");

                    // if (mManageConnectedSocket == null) {
                    mManageConnectedSocket = new ManageConnectedSocket(socket);
                    mManageConnectedSocket.start();
                    //}
                    //}
                } catch (IOException e) {
                    //  mesgText.setText("BT socket closed or timeout");
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    /**
     * Client
     */
    private class BtClient extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        BluetoothAdapter mBluetoothAdapter;


        UUID mUUID;

        public BtClient(BluetoothDevice device, BluetoothAdapter myBluetoothAdapter, UUID MY_UUID) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.

            mmDevice = device;
            mUUID = MY_UUID;

            mBluetoothAdapter = myBluetoothAdapter;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                mmSocket = device.createRfcommSocketToServiceRecord(mUUID);

                Log.v(TAG, "Client get BT remote server : " + mmSocket.getRemoteDevice().getName());

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Log.v(TAG, "Client try to connected to server");

                mmSocket.connect();

                Log.v(TAG, "Client socket connected");

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.

                mySocketManager = new ManageConnectedSocket(mmSocket);
                mySocketManager.start();

            } catch (IOException connectException) {

                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    /**
     *
     */
    public class ManageConnectedSocket extends Thread {

        private Handler mHandler; // handler that gets info from Bluetooth service
        private final BluetoothSocket mmSocket;
        private byte[] mmBuffer; // mmBuffer store for the stream

        boolean RUNNING = true;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        public ManageConnectedSocket(BluetoothSocket socket) {

            mmSocket = socket;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = mmSocket.getInputStream();

                Log.v(TAG, "Get In socket : " + tmpIn.toString());

            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = mmSocket.getOutputStream();
                Log.v(TAG, "Get Out socket : " + tmpOut.toString());
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
        }

        public void run() {
            mmBuffer = new byte[1024];
            int len;

            ByteArrayOutputStream outStr = new ByteArrayOutputStream();
            String data = new String("");

            String remoteName = mmSocket.getRemoteDevice().getName();

            // Read data from remote devices
            while (RUNNING) {
                try {

                    Log.v(TAG, "Read socket wait data ");

                    len = tmpIn.read(mmBuffer);

                    outStr.write(mmBuffer, 0, len);


                    Log.v(TAG, "Read socket =" + outStr.toString("UTF-8") + "***");


                    if (outStr.toString("UTF-8").equals("stop")) {
                        Log.v(TAG, "End of file");

                        refreshUI("File received !");

                        writeToFile(data);

                        tmpIn.close();
                        tmpOut.close();

                        outStr.close();

                        RUNNING = false;
                    } else {
                        data += outStr.toString("UTF-8");
                    }

                    outStr.reset();

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Write data to remote devices
        public void write(String msg) {

            try {
                Log.v(TAG, "Write socket : " + msg);

                tmpOut.write(msg.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void stopManagedSocket() {
            RUNNING = false;
        }


        // Close the socket
        public void cancel() {
            try {
                mmSocket.close();
                Log.d(TAG, "Input stream was closed");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    /**
     * @param data
     */
    private void writeToFile(String data) {
        try {

            Log.v(TAG, "file = " + data);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(DataManager.xmlFileOther, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }



    private void refreshUI(String msg) {

        final String mMsg = msg;

        runOnUiThread(new Runnable() {
            public void run () {
                mesgText.setText(mMsg);
            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtServer != null)
            mBtServer.cancel();

        if (mBtClient != null)
            mBtClient.cancel();

        unregisterReceiver(mReceiver);
    }

}


