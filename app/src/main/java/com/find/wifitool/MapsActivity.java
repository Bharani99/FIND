package com.find.wifitool;

import android.animation.FloatArrayEvaluator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.find.wifitool.internal.Constants;
import com.qozix.tileview.TileView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends AppCompatActivity {

    XTileView tileView;

    private ImageView myPosition;
    private String pathId;
    private HashMap<String, Integer> counter;
    private Timer timer;
    static String start;
    public HashMap<String, MapGraph.State> stateMap = new HashMap<>();
    static int ifNavigated;


    /*



     */


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String currLocation = intent.getStringExtra("location");

            Log.i("Broadcast","hey");
        }
    };


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

    /*
    // Takes the document and returns the list of the pictures
    private ArrayList<NavigableItem> getPictures(Document document)
    {
        NodeList nodes = document.getElementsByTagName("picture");

        String name, author, description, sensor, room;
        ArrayList<NavigableItem> pics = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);

            name = element.getElementsByTagName("name").item(0).getTextContent();
            author = element.getElementsByTagName("author").item(0).getTextContent();
            description = element.getElementsByTagName("description").item(0).getTextContent();
            sensor = element.getElementsByTagName("id").item(0).getTextContent();
            room = multigraph.getConnectedState(Parameters.SENSORS, sensor).id;

            Log.d("Test"," "+name+" "+description);

            NavigableItem picture = new NavigableItem(name, author, description, sensor, room);
            pics.add(picture);
        }

        return pics;
    }


    // Initialize the Kontakt beacon manager
    // It was chosen the monitoring mode (instead ranging). It give us more result stability and
    // less energy consumption
    private void initialize()
    {
        beaconManager = BeaconManager.newInstance(this);
        // LOW_POWER allows to save device's energy consumption
        beaconManager.setScanMode(BeaconManager.SCAN_MODE_LOW_POWER);
        beaconManager.setRssiCalculator(RssiCalculators.newLimitedMeanRssiCalculator(5));
        beaconManager.setBeaconActivityCheckConfiguration(BeaconActivityCheckConfiguration.DEFAULT);
        // distance sort is set to ascending order. In this way the first beacon will be the
        // closest one
        beaconManager.setDistanceSort(BeaconDevice.DistanceSort.ASC);

        beaconManager.registerRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(final Region region, final List<BeaconDevice> beacons) {
                MapsActivity.this.runOnUiThread(new Thread() {
                    @Override
                    public void run() {
                        if (!beacons.isEmpty()) {
                            //getRoom(beacons.get(0).getUniqueId());
                            if (counter.containsKey(beacons.get(0).getUniqueId()))
                                counter.put(beacons.get(0).getUniqueId(), new Integer(counter.get(beacons.get(0).getUniqueId())).intValue() + 1);
                            else
                                counter.put(beacons.get(0).getUniqueId(), new Integer(1));
                        }
                    }
                });
            }
        });

    }

    */

    // Find a path from actual location to the selected room and draw it
    private void navigation(NavigableItem item) {
        // check if there is already a path drawn and, if it is the case, the latter is removed
        if (tileView.getPathsDrawn() > 0)
            tileView.removeNavigablePath(pathId);

        MapGraph.State destination = multigraph.getState("R80");
        if (!"R11".equals(destination.id)) {
            List<MapGraph.State> path = multigraph.getPath(1, "R11", destination.id);
            List<double[]> positions = new ArrayList<>();
            for (MapGraph.State s : path) {
                //s = changeCoords(s);
                positions.add(new double[]{s.coords[0], s.coords[1]});
            }
            //positions.remove(0);

            pathId = mySensor.id + "-" + destination.id;
            tileView.drawNavigablePath(pathId, new XTileView.NavigablePath(positions),0);


        }
    }

    private void navigation(String destine) {
        // check if there is already a path drawn and, if it is the case, the latter is removed

        Toast.makeText(this," "+tileView.getPathsDrawn(),Toast.LENGTH_SHORT).show();
        if (tileView.getPathsDrawn() > 0)
            tileView.removeNavigablePath(pathId);

        MapGraph.State destination = multigraph.getState(destine);
        if (!"R11".equals(destination.id)) {
            SharedPreferences         sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, 0);

            start = sharedPreferences.getString("CurrentLocation","R65").toString();

            ifNavigated=1;

            start=            start.substring(1,start.length()-1);


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
            tileView.drawNavigablePath(pathId, new XTileView.NavigablePath(positions),0);


        }
    }

    // check if you've reached the destination
    private void checkForDestination(double x, double y)
    {
        if(tileView.getPathsDrawn() > 0) {
            // get destination coordinates
            XTileView.NavigablePath navP = tileView.getNavigablePath(pathId);
            double[] dest = navP.points.get(navP.points.size() - 1);

            if ((x == dest[0]) && (y == dest[1])) {
                tileView.removeNavigablePath(pathId);
                Toast.makeText(getApplicationContext(), ("Destination reached!"),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Initialize timer method
    final Runnable setMaxCount = new Runnable() {
        public void run() {
            String maxId = null;
            int maxCount = 0;
            for(String key : counter.keySet()){
                if(counter.get(key).intValue() > maxCount){
                    maxCount = counter.get(key).intValue();
                    maxId = key;
                }
            }
            counter.clear();
            if(maxId == null && myPosition != null)
                tileView.removeZoomableMarker("blue_dot");
           // else
             //   getRoom(maxId);
        }
    };

    TimerTask task = new TimerTask(){
        public void run() {
            runOnUiThread(setMaxCount);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myPosition = null;
        counter = new HashMap<>();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Constants.TRACK_BCAST));
        // Load gml file and create graphs
        Document document = null;
        try {
            InputStream xml = this.getAssets().open("1dd.gml");
            XMLParser parser = new XMLParser(xml);
            document = parser.getDocument();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
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


    /*    MapGraph.State sensor0 = sensorGraph.getState("SS0");
        MapGraph.State sensor1 = sensorGraph.getState("SS1");
        MapGraph.State sensor2 = sensorGraph.getState("SS2");
        MapGraph.State sensor3 = sensorGraph.getState("SS3");
        MapGraph.State sensor4 = sensorGraph.getState("SS4");
        MapGraph.State sensor5 = sensorGraph.getState("SS5");
        MapGraph.State sensor6 = sensorGraph.getState("SS6");
        MapGraph.State sensor7 = sensorGraph.getState("SS7");
        MapGraph.State sensor8 = sensorGraph.getState("SS8");
        MapGraph.State sensor9 = sensorGraph.getState("SS9");

        // Set interlayer connections (room <-> sensor)
        multigraph.addInterConnection(room0, sensor0);
        multigraph.addInterConnection(room0, sensor1);
        multigraph.addInterConnection(room0, sensor2);
        multigraph.addInterConnection(room2, sensor3);
        multigraph.addInterConnection(room1, sensor4);
        multigraph.addInterConnection(room1, sensor5);
        multigraph.addInterConnection(room6, sensor6);
        multigraph.addInterConnection(room7, sensor7);
        multigraph.addInterConnection(room4, sensor8);
        multigraph.addInterConnection(room3, sensor9);

        // Create navigable items
        Document doc = null;
        try {
            InputStream xml = this.getAssets().open(Parameters.ITEM_FILE);
            XMLParser parser = new XMLParser(xml);
            doc = parser.getDocument();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
        }

*/
        // Create a TileView object
        tileView = new XTileView( this );

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


        final EditText destination=new EditText(this);
        destination.setHint("Enter Destination");

        Button button=new Button(this);
        button.setText("Go");


        FrameLayout frameLayout=new FrameLayout(this);
        frameLayout.addView(tileView);

        setContentView(frameLayout);


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
                String desti=destination.getText().toString().toUpperCase();




                navigation(desti);
            }
        });




     /*   List<double[]> pos=new ArrayList<>();

        Log.d("sd"," "+this.getPackageName());

        pos.add(new double[] {stateMap.get("R11").coords[0],stateMap.get("R11").coords[1]});
        pos.add(new double[] {stateMap.get("R87").coords[0],stateMap.get("R87").coords[1]});
        pos.add(new double[] {stateMap.get("R39").coords[0],stateMap.get("R39").coords[1]});
        pos.add(new double[] {stateMap.get("R49").coords[0],stateMap.get("R49").coords[1]});
        pos.add(new double[] {stateMap.get("R81").coords[0],stateMap.get("R81").coords[1]});
        pos.add(new double[] {stateMap.get("R8").coords[0],stateMap.get("R8").coords[1]});

        Log.d("sdd"," "+pos.toString());*/

        //tileView.drawNavigablePath("12", new XTileView.NavigablePath(pos));

        // Initialize Kontakt
        // Initialize timer

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_drawer, menu);
        return true;
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Check if Bluetooth is enabled on the device
      /*  if(!beaconManager.isBluetoothEnabled()){
            final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Parameters.REQUEST_ENABLE_BT);
        } else if(beaconManager.isConnected()){
            startRanging();
        } else {
            connect();
        }*/
    }

    @Override
    protected void onPause(){
        super.onPause();
       /* if(beaconManager.isConnected()){
            beaconManager.stopRanging();
        }*/
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    /*    beaconManager.disconnect();
        beaconManager = null;*/
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null)
            return;

      /*  if(requestCode == Parameters.REQUEST_ENABLE_BT) {
            if(resultCode != Activity.RESULT_OK){
                Toast.makeText(getApplicationContext(),getString(R.string.bluetooth_not_enable), Toast.LENGTH_SHORT).show();
            }*/
        if(requestCode == 666) {
            // Check for selected item in Search Activity
            // If there is, show navigation from actual location to the selected item
            ArrayList<NavigableItem> selected = data.getParcelableArrayListExtra("Search");
            navigation(selected.get(0));
        }
    }
/*
    private void startRanging(){
        try{
            beaconManager.startRanging();
        }catch (RemoteException e){
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connect() {
        try{
            beaconManager.connect(new OnServiceBoundListener() {
                @Override
                public void onServiceBound() throws RemoteException {
                    beaconManager.startRanging();
                }
            });
        } catch (RemoteException e){
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
*/


    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_localize) {
            // add marker for my position
            if (myPosition != null) {
                tileView.forceZoom(0.75f);
                tileView.moveToMarker(myPosition, true);
            }
            return true;
        }else if (id == R.id.action_search){
            if(myRoom == null)
                return true;
            Intent nextActivityIntent = new Intent(this, SearchActivity.class);
            // put the list of pictures
            nextActivityIntent.putParcelableArrayListExtra("Pictures", pictures);
            startActivityForResult(nextActivityIntent, 666);
        }else if (id == R.id.action_info)
        {
                if((mySensor != null) && (!checkForPicture()))
                    Toast.makeText(getApplicationContext(), getString(R.string.no_pictures), Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

*/
    // Check if there is a picture in the actual location

    // Return sensor id <-> beacon id


    // Image has origin coordinates in Top Left,
    // map has origin coordinates in Bottom Left, so y must be changed


    // When the monitor period is over, the device location is updated
    // The position dot is moved to the new position, if it was changed
    // The picture's information is shown, if there is a picture close to the device
 /*   private void getRoom(String id)
    {


        myRoom = multigraph.getConnectedState(Parameters.SENSORS, sensorId);
        mySensor = multigraph.getState(Parameters.SENSORS, sensorId);
        mySensor = changeCoords(mySensor);

        if((prevSensor == null) || (!prevSensor.id.equals(mySensor.id))){
            checkForPicture();
            prevSensor = mySensor;
        }

       if((prevRoom == null) || (!prevRoom.label.equals(myRoom.label))){
            Toast.makeText(getApplicationContext(), myRoom.label, Toast.LENGTH_SHORT).show();
            prevRoom = myRoom;
        }

        // Add position dot
        if (myPosition == null) {
            myPosition = new ImageView(getApplicationContext());
            myPosition.setImageResource(R.drawable.blue_dot_7);
            tileView.addZoomableMarker(myPosition, "blue_dot", mySensor.coords[0], mySensor.coords[1]);
            tileView.forceZoom(0.75f);
            tileView.moveToMarker(myPosition, true);
        }else {
            tileView.moveMarker("blue_dot", mySensor.coords[0], mySensor.coords[1]);
            checkForDestination(mySensor.coords[0], mySensor.coords[1]);
        }
    }*/
}