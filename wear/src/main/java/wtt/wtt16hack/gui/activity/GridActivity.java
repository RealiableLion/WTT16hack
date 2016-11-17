package wtt.wtt16hack.gui.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import watch.nudge.gesturelibrary.AbstractGestureClientActivity;
import watch.nudge.gesturelibrary.GestureConstants;
import wtt.wtt16hack.R;
import wtt.wtt16hack.gui.adapter.MyViewPagerAdapter;
import wtt.wtt16hack.gui.fragment.CustomFragment;

/**
 * Created by Davide on 12/11/2016.
 */

public class GridActivity extends AbstractGestureClientActivity {

    private static final long CONNECTION_TIME_OUT_MS = 1000;
    private static final String MESSAGE = "GESTURE";
    private GoogleApiClient client;
    private String nodeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);

        initApi();

        final ViewPager mViewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(mViewPager);
        setAmbientEnabled();
        setSubscribeWindowEvents(false);
    }

    private void setupViewPager(ViewPager viewPager){
        List<String> text = new ArrayList<>();
        text.add("Monitoring");
        text.add("Gesture");
        text.add("Gesture 2");
        MyViewPagerAdapter mAdapter = new MyViewPagerAdapter(getFragmentManager(), text);
        CustomFragment mScenarioFragment = new CustomFragment();
        CustomFragment mMonitoringFragment = new CustomFragment();

        viewPager.setAdapter(mAdapter);
    }

    @Override
    public ArrayList<GestureConstants.SubscriptionGesture> getGestureSubscpitionList() {
        ArrayList<GestureConstants.SubscriptionGesture> gestures = new ArrayList<GestureConstants.SubscriptionGesture>();
        gestures.add(GestureConstants.SubscriptionGesture.FLICK);
        gestures.add(GestureConstants.SubscriptionGesture.SNAP);
        gestures.add(GestureConstants.SubscriptionGesture.TWIST);

        return gestures;
    }

    @Override
    public boolean sendsGestureToPhone() {
        return true;
    }

    @Override
    protected void setWatchOnLeftHand(boolean watchOnLeftHand) {
        super.setWatchOnLeftHand(true);
    }

    @Override
    public void onSnap() {
        Toast.makeText(this,"Snap it up",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFlick() {
        Toast.makeText(this,"Got a flick!",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTwist() {
        Toast.makeText(this,"Just twist it",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGestureWindowClosed() {
        Toast.makeText(this,"Gesture window closed.",Toast.LENGTH_LONG).show();
    }

    //These functions won't be called until you subscribe to the appropriate gestures.

    @Override
    public void onTiltX(float x) {
        throw new IllegalStateException("This function should not be called unless subscribed to TILT_X.");
    }

    @Override
    public void onTilt(float x, float y, float z) {
        throw new IllegalStateException("This function should not be called unless subscribed to TILT.");
    }

    private void initApi() {
        Log.d("###à","1");
        client = getGoogleApiClient(this);
        Log.d("###à","2");
        retrieveDeviceNode();
    }


    /**
     * Returns a GoogleApiClient that can access the Wear API.
     * @param context
     * @return A GoogleApiClient that can make calls to the Wear API
     */
    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    /**
     * Connects to the GoogleApiClient and retrieves the connected device's Node ID. If there are
     * multiple connected devices, the first Node ID is returned.
     */
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectionResult cr = client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                }
                client.disconnect();
            }
        }).start();
    }

    /**
     * Sends a message to the connected mobile device, telling it to show a Toast.
     */
    public void sendaMsgTOPhone(final String message) {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(client, nodeId, message, null);
                    Log.d("MANDATO", "MANDATO");
                    client.disconnect();
                }
            }).start();
        }
        else
            Toast.makeText(getApplicationContext(), "No node here!", Toast.LENGTH_SHORT).show();

    }

}
