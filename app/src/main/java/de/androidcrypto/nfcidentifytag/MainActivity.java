package de.androidcrypto.nfcidentifytag;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    TextView nfcaContent;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcaContent = findViewById(R.id.tvNfcaContent);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");

        NfcA nfca = null;

        // Whole process is put into a big try-catch trying to catch the transceive's IOException
        try {
            nfca = NfcA.get(tag);
            if (nfca != null) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(200);
            } else {
                writeToUiToast("The tag is not readable with NFCA protocol");
                return;
            }

            nfca.connect();
            String nfcaContentString = "Content of NFC tag";

            // first get sak
            short sakData = nfca.getSak();
            nfcaContentString = nfcaContentString + "\n" + "read SAK";
            nfcaContentString = nfcaContentString + "\n" + "sakData: " + shortToHex(sakData);

            // then check atqa
            byte[] atqaData = nfca.getAtqa();
            nfcaContentString = nfcaContentString + "\n" + "read ATQA";
            nfcaContentString = nfcaContentString + "\n" + "atqaData: " + bytesToHex(atqaData);

            String ntagType = NfcIdentifyNtag.checkNtagType(nfca, tag.getId()); // get the NTAG-id along with other data
            nfcaContentString = nfcaContentString + "\n" + "NTAG TYPE: " + ntagType;
            if (!ntagType.equals("0")) {
                nfcaContentString = nfcaContentString + "\n" + "complete NTAG TYPE: " + NfcIdentifyNtag.getIdentifiedNtagType();
                nfcaContentString = nfcaContentString + "\n" + "NTAG pages: " + NfcIdentifyNtag.getIdentifiedNtagPages();
                nfcaContentString = nfcaContentString + "\n" + "NTAG memory bytes: " + NfcIdentifyNtag.getIdentifiedNtagMemoryBytes();
                nfcaContentString = nfcaContentString + "\n" + "NTAG ID: " + bytesToHex(NfcIdentifyNtag.getIdentifiedNtagId());
            }

            String[] techList = tag.getTechList();
            for (int i = 0; i < techList.length; i++) {
                nfcaContentString = nfcaContentString + "\n" + "techlist: " + techList[i];
            }

            String finalNfcaContentString = nfcaContentString;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText(finalNfcaContentString);
                }
            });

            try {
                nfca.close();
            } catch (IOException e) {
                writeToUiToast("IOException: " + e);
                e.printStackTrace();
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiToast("ERROR Tag lost exception");
        } catch (IOException e) {
            //Trying to catch any ioexception that may be thrown
            e.printStackTrace();
            writeToUiToast("IOException: " + e);
        } catch (Exception e) {
            //Trying to catch any exception that may be thrown
            e.printStackTrace();
            writeToUiToast("Exception: " + e);
        }
    }
/*
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
*/
    public static String shortToHex(short data) {
        return Integer.toHexString(data & 0xffff);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    private void writeToUiToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

}