package com.find.wifitool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.find.wifitool.internal.Constants;
import com.find.wifitool.internal.Utils;
import com.find.wifitool.wifi.WifiIntentReceiver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

/**
 * Created by bharani on 05/03/18.
 */

public class NavigateFragment extends Fragment {

    private static final String TAG = TrackFragment.class.getSimpleName();

    //   TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    //private variables
    private OnFragmentInteractionListener mListener;
    private Context mContext;
    SharedPreferences sharedPreferences;
    private String strUsername;
    private String strServer;
    private String strGroup;
    private String strLocation = null;  // We don't need any location value fr Tracking
    private int trackVal;

    Handler handler = new Handler();


    // Timers to keep track of our Tracking period
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= 23 ) {
                if(Utils.isWiFiAvailable(mContext) && Utils.hasAnyLocationPermission(mContext)) {
                    Intent intent = new Intent(mContext, WifiIntentReceiver.class);
                    intent.putExtra("event", Constants.TRACK_TAG);
                    intent.putExtra("groupName", strGroup);
                    intent.putExtra("userName", strUsername);
                    intent.putExtra("serverName", strServer);
                    intent.putExtra("locationName", sharedPreferences.getString(Constants.LOCATION_NAME, ""));
                    mContext.startService(intent);
                }
            }
            else if (Build.VERSION.SDK_INT < 23) {
                if(Utils.isWiFiAvailable(mContext)) {
                    Intent intent = new Intent(mContext, WifiIntentReceiver.class);
                    intent.putExtra("groupName", strGroup);
                    intent.putExtra("userName", strUsername);
                    intent.putExtra("serverName", strServer);
                    intent.putExtra("locationName", sharedPreferences.getString(Constants.LOCATION_NAME, ""));
                    mContext.startService(intent);
                }
            }
            else {
                return;
            }
            handler.postDelayed(runnableCode, trackVal * 1000);
        }
    };



    //Imported

    XTileView tileView;
    static String desti;
    private ImageView myPosition;
    private String pathId;
    private HashMap<String, Integer> counter;
    private Timer timer;
    static String start;
    public HashMap<String, MapGraph.State> stateMap = new HashMap<>();
    static int ifNavigated;



    public MultilayerMapGraph multigraph;
    public MapGraph.State myRoom, prevRoom = null, mySensor, prevSensor = null;

    // Takes GML document and graph's layer and returns the graph
    // For the information about the document format see the indoorGML standard
    private MapGraph createGraph(Document document, int level) {
        Element multiLayeredGraph = (Element) document.getElementsByTagName("MultiLayeredGraph").item(0);
        Element spaceLayers = (Element) multiLayeredGraph.getElementsByTagName("spaceLayers").item(0);
        NodeList spaceLayersMembers = spaceLayers.getElementsByTagName("spaceLayerMember");

        Element roomSpaceLayerMember = (Element) spaceLayersMembers.item(level);
        Element roomSpaceLayer = (Element) roomSpaceLayerMember.getElementsByTagName("SpaceLayer").item(0);
        // get nodes
        Element roomNodes = (Element) roomSpaceLayer.getElementsByTagName("nodes").item(0);
        Log.e("rooms"," "+roomNodes.toString());
        // get edges
        Element roomEdges = (Element) roomSpaceLayer.getElementsByTagName("edges").item(0);

        Log.d("edges", " a "+roomEdges);
        // get stateMember and get informations
        NodeList roomStateMembers = roomNodes.getElementsByTagName("stateMember");
        // define temp vars and get state's informations
        Element stateMemb, state, gmlGeom, gmlPoint;
        String id, label, tempStr;
        float coords[] = new float[2];
        for (int i = 0; i < roomStateMembers.getLength(); i++) {
            stateMemb = (Element) roomStateMembers.item(i);
            state = (Element) stateMemb.getElementsByTagName("State").item(0);
            // get id
            id = state.getAttribute("gml:id");
            // get label
            label = state.getElementsByTagName("gml:name").item(0).getFirstChild().getTextContent();
            // get coordinates
            Log.d("teststart","asd");
            gmlGeom = (Element) state.getElementsByTagName("geometry").item(0);
            gmlPoint = (Element) gmlGeom.getElementsByTagName("gml:Point").item(0);
            tempStr = gmlPoint.getElementsByTagName("gml:pos").item(0).getTextContent();
            coords[0] = Float.parseFloat(tempStr.split(" ")[0]);
            coords[1] = (Float.parseFloat(tempStr.split(" ")[1])-2339-50)*(-1);

            Log.d("testend","asd");
            // add state to the list
            stateMap.put(id, (new MapGraph.State(id, label, coords[0], coords[1])));
            System.out.println(id + "\t" + label + "\t" + coords[0] + "\t" + coords[1]);
        }
        // get TransitionMember and get start and end states
        NodeList roomTransitionMembers = roomEdges.getElementsByTagName("transitionMember");
        List<MapGraph.Transition> transList = new ArrayList<>();
        // define temp vars and get transition's informations
        Element transMember, trans, start, end;
        MapGraph.State startState, endState;
        for (int i = 0; i < roomTransitionMembers.getLength(); i++) {
            transMember = (Element) roomTransitionMembers.item(i);
            trans = (Element) transMember.getElementsByTagName("Transition").item(0);
            // get start State
            start = (Element) trans.getElementsByTagName("connects").item(0);
            tempStr = start.getAttribute("xlink:href");
            startState = stateMap.get(tempStr.split("#")[1]);
            // get end State
            end = (Element) trans.getElementsByTagName("connects").item(1);
            tempStr = end.getAttribute("xlink:href");
            endState = stateMap.get(tempStr.split("#")[1]);
            // add transition to list
            System.out.println(startState.label+"\t"+endState.label);

            transList.add(new MapGraph.Transition(startState, endState));
            transList.add(new MapGraph.Transition(endState, startState));

        }
        // construct the Graph
        return new MapGraph(stateMap, transList);
    }


    private void navigation(String destine) {
        // check if there is already a path drawn and, if it is the case, the latter is removed

        Toast.makeText(mContext," "+tileView.getPathsDrawn(),Toast.LENGTH_SHORT).show();
        if (tileView.getPathsDrawn() > 0)
            tileView.removeNavigablePath(pathId);

        Log.d("States"," "+multigraph.getState(destine));
        MapGraph.State destination = multigraph.getState(destine);
        if (!"R11".equals(destination.id)) {
            SharedPreferences         sharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);

            start = sharedPreferences.getString("CurrentLocation","R65").toString();

            ifNavigated=1;



            //start=            start.substring(1,start.length()-1);


            Log.e("Location", start   );



            MapGraph.State startpoint = multigraph.getState(""+start);


            Log.e("Location1","sd "+ startpoint.id   );

            List<MapGraph.State> path = multigraph.getPath(0, startpoint.id, destination.id);
            List<double[]> positions = new ArrayList<>();
            for (MapGraph.State s : path) {
                //s = changeCoords(s);
                positions.add(new double[]{s.coords[0], s.coords[1]});
            }
            //positions.remove(0);

            pathId = "R11" + "-" + destination.id;
            tileView.drawNavigablePath(pathId, new XTileView.NavigablePath(positions));


        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // Required empty public constructor
    public NavigateFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Checking if the Location service is enabled in case of Android M or above users
        if (!Utils.isLocationAvailable(mContext)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setMessage("Location service is not On. Users running Android M and above have to turn on location services for FIND to work properly");
            dialog.setPositiveButton("Enable Locations service", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    getActivity().startActivity(myIntent);
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    logMeToast("Thank you!! Getting things in place.");
                }
            });
            dialog.show();
        }

        // Getting values from Shared prefs for Tracking
        sharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        strGroup = sharedPreferences.getString(Constants.GROUP_NAME, Constants.DEFAULT_GROUP);
        strUsername = sharedPreferences.getString(Constants.USER_NAME, Constants.DEFAULT_USERNAME);
        strServer = sharedPreferences.getString(Constants.SERVER_NAME, Constants.DEFAULT_SERVER);
        trackVal = sharedPreferences.getInt(Constants.TRACK_INTERVAL, Constants.DEFAULT_TRACKING_INTERVAL);


        myPosition = null;
        counter = new HashMap<>();

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter(Constants.TRACK_BCAST));
        // Load gml file and create graphs
        Document document = null;
        try {
            InputStream xml = getActivity().getAssets().open("1dd.gml");
            XMLParser parser = new XMLParser(xml);
            document = parser.getDocument();
        } catch (Exception e) {
            Toast.makeText(getActivity(),e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Create room graph and sensor graph
        MapGraph roomGraph = createGraph(document, 0);

        // Create multilayer graph with room graph and sensor graph
        multigraph = new MultilayerMapGraph(roomGraph);

        // Log.d("MapSize"," "+multigraph.getGraph(0).toString());

        MapGraph.State room0 = roomGraph.getState("ST0");
        MapGraph.State room1 = roomGraph.getState("ST1");
        MapGraph.State room2 = roomGraph.getState("ST2");
        MapGraph.State room3 = roomGraph.getState("ST3");
        MapGraph.State room4 = roomGraph.getState("ST4");
        MapGraph.State room6 = roomGraph.getState("ST6");
        MapGraph.State room7 = roomGraph.getState("ST7");

        // Create a TileView object
        tileView = new XTileView( mContext );

        tileView.setSize( 3309, 2339 );
        tileView.setBackgroundColor( 0xFFe7e7e7 );


        tileView.setScaleLimits( 0, 2 );

        // start small and allow zoom
        tileView.setScale( 0.5f );

        // with padding, we might be fast enough to create the illusion of a seamless image
        tileView.setViewportPadding( 256 );

        // we're running from assets, should be fairly fast decodes, go ahead and render asap
        tileView.setShouldRenderWhilePanning( true );
        // Setup the TileView
        // tileView.setSize(Parameters.TILE_WIDTH, Parameters.TILE_HEIGHT);

        tileView.addDetailLevel( 1f, "plan/0/1000/%d_%d.jpg");
        tileView.addDetailLevel( 0.5f, "plan/0/500/%d_%d.jpg");
        tileView.addDetailLevel( 0.25f, "plan/0/250/%d_%d.jpg");
        tileView.addDetailLevel( 0.125f, "plan/0/125/%d_%d.jpg");


        // Define the bounds using the map size in pixel

        // Display the TileView
        tileView.setScale(0);
        //setContentView(tileView);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_navigate, container, false);

        // Listener to the broadcast message from WifiIntent
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter(Constants.TRACK_BCAST));




        final EditText destination=new EditText(mContext);
        destination.setHint("Enter Destination");

        Button button=new Button(mContext);
        button.setText("Go");


        FrameLayout frameLayout=(FrameLayout) rootView.findViewById(R.id.frame);
                frameLayout.addView(tileView);



        //RelativeLayout linearLayout=new RelativeLayout();
        //frameLayout.addView(linearLayout);

        FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        frameLayout.setLayoutParams(param);
        //param.addRule(frameLayout.ALIGN_PARENT_BOTTOM);

        FrameLayout.LayoutParams lParams = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
        //lParams.weight=3;
        destination.setLayoutParams(lParams);

        //lParams.weight=1;
        button.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM|Gravity.RIGHT));


        frameLayout.addView(destination);

        frameLayout.addView(button);


        //tileView.addView(linearLayout);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                desti=destination.getText().toString().toUpperCase();




                Log.e("Destination","\'"+desti+"\'");
                navigation(desti);
            }
        });

        handler.post(runnableCode);


        // Inflate the layout for this fragment
        return rootView;
    }

    // Getting the CurrentLocation from the received braodcast
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String currLocation = intent.getStringExtra("location");

            SharedPreferences.Editor editor = sharedPreferences.edit();



            editor.putString("CurrentLocation", currLocation);

            editor.commit();

            Log.e("Navigation location",currLocation);

            if(ifNavigated>0&&!currLocation.equals(start)&&!currLocation.equals("false")){start=currLocation;
            navigation(desti);}


        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        handler.removeCallbacks(runnableCode);
        ifNavigated=0;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        mListener = null;
    }

    // Logging message in form of Toasts
    private void logMeToast(String message) {
        Log.d(TAG, message);
        toast(message);
    }

    private void toast(String s) {
        Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
    }
}
