package mag.linux.android_sensors;


import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;



/**
 * @author fredericamps@gmail.com - 2017
 *
 * NfcTagManager
 *
 */
public class NfcTagManager  {


    final String TAG_NFC="sensor";
    Context mContext;

    static boolean NFC_ENABLE=false;
    static boolean NFC_DEVICE=false;

    /**
     *
     * @param context
     */
    NfcTagManager(Context context)
    {
        mContext = context;

        NfcManager manager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();

        if(adapter == null)
        {
            // Call your Alert message
            Toast.makeText(mContext, R.string.NFC_alert, Toast.LENGTH_LONG).show();

            NFC_DEVICE=NFC_ENABLE=false;
        }
        else if (adapter != null && !adapter.isEnabled())
        {
            // Call your Alert message
            Toast.makeText(mContext, R.string.NFC_disable, Toast.LENGTH_LONG).show();

            NFC_ENABLE=false;
            NFC_DEVICE=true;
        }


    }

    /**
     *
     * @param intent
     */
    protected String computeNfcIntent (Intent intent) {

        Parcelable[] rawMsgs = null;

        String action = intent.getAction();

        Log.v(TAG_NFC, "computeNfcIntent = " + action);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_TAG);
        }
        else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            for (String tech : tagFromIntent.getTechList()) {
                Log.v(TAG_NFC, "computeNfcIntent = " + tech);
            }
        }

        if(rawMsgs != null && rawMsgs.length > 0) {
           return NdefComputeData(rawMsgs);
        }
        else {
            Toast.makeText(mContext, "This NFC tag has no NDEF data.", Toast.LENGTH_LONG).show();
        }
        return null;
    }


    /**
     *
     * @param rawMsgs
     * @return
     */
    String NdefComputeData (Parcelable[] rawMsgs) {

        NdefMessage msgs[] = new NdefMessage[rawMsgs.length];

        for (int i = 0; i < rawMsgs.length; i++) {
            msgs[i] = (NdefMessage) rawMsgs[i];

            NdefRecord[] records = msgs[i].getRecords();

            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {

                        nfcInfo(ndefRecord);

                        return computeNdefRecord(ndefRecord);

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }


    /**
     *
     * @param record
     * @return
     * @throws UnsupportedEncodingException
     * @see "NFC forum specification for "Text Record Type Definition" at 3.2.1"
     * @see "http://www.nfc-forum.org/specs/"
     */
    private String computeNdefRecord(NdefRecord record) throws UnsupportedEncodingException {
        byte[] payload = record.getPayload();

        // bit_7 defines encoding - mask 1000 0000
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        // bit_5..0 length of IANA language code - mask 0011 1111
        int languageCodeLength = payload[0] & 0063;

        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        // e.g. "en"

        // Get the payload

        Log.v(TAG_NFC, "Payload = " + new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding));

        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }


    /**
     *
     * @param record
     * @return
     * @throws UnsupportedEncodingException
     */
    private void nfcInfo(NdefRecord record) throws UnsupportedEncodingException {

        if (record == null) return ;

        Log.v(TAG_NFC, "Record =" + record.toString());

        byte[] payload = record.getPayload();

        if (payload == null) return ;

        int languageCodeLength = payload[0] & 0063;

        byte[] tagInfo = record.getType();

        // Most classical Type. @see NdefRcord class for more type
        if(tagInfo[0] == NdefRecord.RTD_TEXT[0])
        {
            Log.v(TAG_NFC, "TYPE = RTD_TEXT ");
        }
        else if(tagInfo[0] == NdefRecord.RTD_URI[0])
        {
            Log.v(TAG_NFC, "TYPE = RTD_URI ");
        }
        else if(tagInfo[0] == NdefRecord.RTD_SMART_POSTER[0] && tagInfo[1] == NdefRecord.RTD_SMART_POSTER[1])
        {
            Log.v(TAG_NFC, "TYPE = RTD_SMART_POSTER ");
        }

        Log.v(TAG_NFC,  "MimeType=" +  record.toMimeType());


        byte []id = record.getId();

        if(id.length>0) {
            for (byte b : id) {
                Log.i(TAG_NFC, "ID = " + String.format("0x%20x", b));
            }
        }
        else
        {
            Log.v(TAG_NFC,  "ID= Not ID defined" );
        }

        // TNF
        Log.v(TAG_NFC,  "TNF=" +  record.getTnf());

        switch (record.getTnf()) {

            case NdefRecord.TNF_EMPTY:
                Log.v(TAG_NFC, "TNF = TNF_EMPTY ");
                break;

            case  NdefRecord.TNF_WELL_KNOWN:
                Log.v(TAG_NFC, "TNF = TNF_WELL_KNOWN ");
                break;

            case NdefRecord.TNF_MIME_MEDIA:
                Log.v(TAG_NFC, "TNF = TNF_MIME_MEDIA ");
                break;

            case NdefRecord.TNF_ABSOLUTE_URI:
                Log.v(TAG_NFC, "TNF = TNF_ABSOLUTE_URI ");
                break;

            case NdefRecord.TNF_EXTERNAL_TYPE:
                Log.v(TAG_NFC, "TNF = TNF_EXTERNAL_TYPE ");
                break;

            case NdefRecord.TNF_UNKNOWN:
                Log.v(TAG_NFC, "TNF = TNF_UNKNOWN ");
                break;

            case NdefRecord.TNF_UNCHANGED:
                Log.v(TAG_NFC, "TNF = TNF_UNCHANGED ");
                break;

            default: Log.v(TAG_NFC, "TNF = not detected ");
                break;
        }

        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        Log.v(TAG_NFC,  "TEXT ENCODING = " + textEncoding);

        int len = payload.length - languageCodeLength - 1;

        Log.v(TAG_NFC,  "TEXT LENGHT=" +  len );

        Log.v(TAG_NFC, new String(payload, 1, languageCodeLength, "US-ASCII"));
    }


    /**
     *
     * @param payload
     * @param locale
     * @param encodeInUtf8
     * @return
     */
    public NdefRecord createTextRecord(String payload, Locale locale, boolean encodeInUtf8) {
   byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII")); // US_ASCII - Seven-bit ASCII
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = payload.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], data);
        return record;
    }


    /**
     *
     * @param tag
     * @param data
     * @return
     * @throws IOException
     * @throws FormatException
     * @throws java.lang.IllegalStateException
     */
    boolean writeTag(Tag tag, String data) throws IOException, FormatException, java.lang.IllegalStateException {

        if(tag==null || data==null || data.trim().length()==0) return false;

        NdefRecord relayRecord = createTextRecord(data, Locale.FRENCH, true);

        // Complete NDEF message with one record
        NdefMessage message = new NdefMessage(new NdefRecord[] {relayRecord});

        Ndef ndef = Ndef.get(tag);

        if(ndef != null) {
            try {

                // tag connection
                if(!ndef.isConnected()) {
                    ndef.connect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Make sure the tag is writable
            if(!ndef.isWritable()) {
                Toast.makeText(mContext, "NFC NOT WRITTABLE", Toast.LENGTH_LONG).show();
                return false;
            }

            // Check if there's enough space on the tag for the message
            int size = message.toByteArray().length;
            if(ndef.getMaxSize() < size) {

                Toast.makeText(mContext, "NO SPACE, AVAILABLE MEMORY : "+ ndef.getMaxSize() + " MESSAGE SIZE : " + size, Toast.LENGTH_LONG).show();

                return false;
            }

            // Write the data to the tag
            try {
                if(ndef.isConnected()) {
                    ndef.writeNdefMessage(message);
                    ndef.close();
                    Toast.makeText(mContext, "NFC TAG WRITE OK", Toast.LENGTH_LONG).show();

                    Log.v(TAG_NFC,  "WRITEN PAYLOAD SIZE = " +data.length());
                    Log.v(TAG_NFC,  "TOTAL MSG SIZE = " +message.getByteArrayLength());
                }
                else
                {
                    Toast.makeText(mContext, "NFC NOT CONNECTED", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {

                e.printStackTrace();
            }

            return true;
        }
        else
        {
            // try to format the Tag in NDEF
            NdefFormatable nForm = NdefFormatable.get(tag);
            if (nForm != null) {
                nForm.connect();
                nForm.format(message);
                nForm.close();
            }
        }
        return false;
    }



    /**
     *
     * @param bs
     * @return
     */
    private static StringBuilder bytesToString(byte[] bs) {
        StringBuilder s = new StringBuilder();
        for (byte b : bs) {
            s.append(String.format("%02X", b));
        }
        return s;
    }

}

