package wtt.wtt16hack;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import de.greenrobot.event.EventBus;
import watch.nudge.phonegesturelibrary.AbstractPhoneGestureActivity;


//phone lib
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

//SMS
import android.telephony.SmsManager;

//location
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.android.gms.wearable.MessageEvent;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import static java.lang.Math.round;


public class MainActivity extends AbstractPhoneGestureActivity {
    private LocationListener locationListener = null;

    public JsonManager containerDati;

    private Button button;

    private static String PhoneNumber = "+393451760653";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         onClickSalva();

        containerDati = new JsonManager(getApplicationContext());
        if (containerDati.isUserRegistered()) {
            setContentView(R.layout.activity_main2);
            TextView nomeLabel = (TextView) findViewById(R.id.nome2);
            nomeLabel.setText(containerDati.readField("name"));
        } else {
            setContentView(R.layout.activity_main);
        }
        //EventBus.getDefault().register(this);

        //phone
        PhoneCallListener phoneCallListener = new PhoneCallListener();
        TelephonyManager telManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        telManager.listen(phoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

//Override these functions to make your app respond to gestures.

    @Override
    public void onSnap() {
        Toast.makeText(this, "Feeling snappy!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFlick() {
        Toast.makeText(this,"Flick that thang and... TEXT!",Toast.LENGTH_LONG).show();

        sendToServer("Emergenza");

        //location
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Location location;
        double longitude = 0;
        double latitude = 0;

        if (isGPSEnabled && isNetworkEnabled) {
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        }

        String smsText = "192.168.43.158/wtt/sos.php?id=" + containerDati.readField("name");
        //sms
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(PhoneNumber, null, smsText, null, null);
    }

    @Override
    public void onTwist() {
        sendSMS();
        sendToServer("Emergenza");

        Toast.makeText(this,"Twistin' the night away and... CALL",Toast.LENGTH_LONG).show();
        Intent phoneCallIntent = new Intent(Intent.ACTION_CALL);
        phoneCallIntent.setData(Uri.parse("tel:" + PhoneNumber));
        startActivity(phoneCallIntent);

    }

    // monitor phone call states
    private class PhoneCallListener extends PhoneStateListener {

        String TAG = "LOGGING PHONE CALL";

        private boolean phoneCalling = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (TelephonyManager.CALL_STATE_RINGING == state) {
                // phone ringing
                Log.i(TAG, "RINGING, number: " + incomingNumber);
            }

            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                // active
                Log.i(TAG, "OFFHOOK");

                phoneCalling = true;
            }

            // When the call ends launch the main activity again
            if (TelephonyManager.CALL_STATE_IDLE == state) {

                Log.i(TAG, "IDLE");

                if (phoneCalling) {

                    Log.i(TAG, "restart app");

                    // restart app
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(
                                    getBaseContext().getPackageName());

                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);

                    phoneCalling = false;
                }

            }
        }
    }


//These functions won't be called until you subscribe to the appropriate gestures
//in a class that extends AbstractGestureClientActivity in a wear app.

    @Override
    public void onTiltX(float x) {
        throw new IllegalStateException("This function should not be called unless subscribed to TILT_X.");
    }

    @Override
    public void onTilt(float x, float y, float z) {
        throw new IllegalStateException("This function should not be called unless subscribed to TILT.");
    }

    @Override
    public void onWindowClosed() {
        Log.e("MainActivity", "This function should not be called unless windowed gesture detection is enabled.");
    }

    //Restituisce un vettore di due valori double il primo e latitudine e il secondo longitudine
    public double[] coordinateGPS() {

        //location
        double loc[] = new double[2];
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Richiesta permessi", Toast.LENGTH_SHORT).show();
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Location location;

        if (isGPSEnabled) {
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    loc[0] = round(location.getLatitude() * 10000) / 10000.0;
                    loc[1] = round(location.getLongitude() * 10000) / 10000.0;
                }
            }
        }
        //Toast.makeText(this, "Help me at (" + String.valueOf(loc[0]) + ", " + String.valueOf(loc[1]) + ")", Toast.LENGTH_SHORT).show();

        return loc;
    }
    //onClick listener del pulsante Salva per salvare dati relativi all'utente
    public void onClickSalva()  {
        Log.d("ENTRATO", "ENTRATO");

//        EditText nome = (EditText) findViewById(R.id.nome);
//        EditText cognome = (EditText) findViewById(R.id.cognome);
//        EditText sangue = (EditText) findViewById(R.id.sangue);
//        EditText eta = (EditText) findViewById(R.id.eta);
//        EditText peso = (EditText) findViewById(R.id.peso);
//        EditText sesso = (EditText) findViewById(R.id.sesso);

        String nome = "Mario";
        String cognome = "Rossi";
        String sangue = "A+";
        String eta = "58";
        String peso = "77";
        String sesso = "M";

        JsonManager containerDati = new JsonManager(getApplicationContext());
        containerDati.registerUser(nome, cognome, sangue, Integer.parseInt(eta), sesso, Integer.parseInt(peso));

        setContentView(R.layout.activity_main2);
        TextView nomeLabel = (TextView) findViewById(R.id.nome2);
        nomeLabel.setText(containerDati.readField("name"));
        Log.d("ENTRATO", "ENTRATO");

    }

    public void onClickChangeActivity(View view) {
        setContentView(R.layout.activity_main);

    }


    public void sendToServer(String string) {
        AsyncHttpClient client = new AsyncHttpClient();
        double[] coord = coordinateGPS();
        string="Tipo di segnalazione: "+string;

        try {
            StringEntity entity = new StringEntity(containerDati.getJSONStringToSend(coord[0],coord[1],string,"+393924953670"));
            client.post(getApplicationContext(), "http://192.168.43.158/wtt/server.php", entity, "application/json", new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int a, Header[] b, final byte[] d) {
                    Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();

                }


                @Override
                public void onFailure(int a, Header[] b, byte[] d, Throwable e) {
                    Toast.makeText(getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
                }


                @Override
                public void onFinish() {
                    Toast.makeText(getApplicationContext(), "Finish", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {

        }

    }





    public void onClickTestServer(View view) {
        AsyncHttpClient client = new AsyncHttpClient();
        double[] coord = coordinateGPS();

        try {
            StringEntity entity = new StringEntity(containerDati.getJSONStringToSend(coord[0],coord[1],"+393924953670"));
            client.post(getApplicationContext(), "http://192.168.43.158/wtt/server.php", entity, "application/json", new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int a, Header[] b, final byte[] d) {
                    Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();

                }


                @Override
                public void onFailure(int a, Header[] b, byte[] d, Throwable e) {
                    Toast.makeText(getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
                }


                @Override
                public void onFinish() {
                    Toast.makeText(getApplicationContext(), "Finish", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {

        }

    }

//    public void onEvent(String message){
//        sendToServer(message);
//    }

    public void sendSMS(){
        String smsText = "Help me! Open: http://192.168.43.158/wtt/sos.php?id=" + containerDati.readField("name");
        //sms
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(PhoneNumber, null, smsText, null, null);
    }
}