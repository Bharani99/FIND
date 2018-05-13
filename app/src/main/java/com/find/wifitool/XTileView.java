package com.find.wifitool;

// eXtended TileView class: it allows to add markers whose images change according to
// the current zoom level. You must define the thresholds and puts the images in the drawable
// folder with their corresponding names.
// Moreover, given a set of points as input, it allows to draw a path composed by arrows.

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.qozix.tileview.TileView;
import com.qozix.tileview.markers.MarkerLayout;

import java.util.HashMap;
import java.util.List;

public class XTileView extends TileView
{

    ImageView marker1;


    HashMap<String, ImageView> arrow=new HashMap<>();
    public static final double ZOOM_3 = 0.35;
    public static final double ZOOM_4 = 0.40;
    public static final double ZOOM_5 = 0.5;
    public static final double ZOOM_6 = 0.6;
    public static final double ZOOM_7 = 0.7;
    public static final double ZOOM_8 = 0.8;
    // ***********************
    // * Marker nested class *
    // ***********************
	// A new Marker is created whenever a new zoomable object is added to the 
	// current XTileView object.
    private class Marker
    {
        public ImageView imageView;
        public String imageBaseName;
        public double[] coords;
        public double[] shifts;

        public Marker(ImageView imageView, String imageBaseName, double coordX, double coordY)
        {
            this.imageView = imageView;
            this.imageBaseName = imageBaseName;
            coords = new double[2];
            coords[0] = coordX;
            coords[1] = coordY;
            shifts = new double[2];
        }

    }


    // ******************
    // * Navigable Path *
    // ******************
	// Basically, it is a list of couple of points.
	// It is created for the sake of semplicity to handle multiple 'NavigablePath' objects.
    public static class NavigablePath
    {
        public List<double[]> points;

        public NavigablePath(List<double[]> points)
        {
            this.points = points;
        }
    }


    // **************************
    // * XTileViewEventListener *
    // **************************
	// Whenever the scale changes, for each Marker that is in the 'markerList', the image is
	// changed according the current zoom level, the shift vector is updated and the marker's
	// position is moved in order to keep the latter centered in the previous position.

        public void onScaleChanged(double scale)
        {
            XTileView extObj = XTileView.this;
            ImageView tmp;
            int resId;


            // get the current zoom level
            String zoomLevel = XTileView.getZoomLevel(scale);

            // change all zoomable markers
            for(String key : extObj.markerList.keySet())
            {
                tmp = (ImageView)extObj.markerList.get(key);
                resId = getResources().getIdentifier(key + "_" + zoomLevel, "drawable", "com.find.wifitool");

                tmp.setImageResource(resId);
                BitmapDrawable bd=(BitmapDrawable) getResources().getDrawable(resId);


                MarkerLayout.LayoutParams layoutParams = (MarkerLayout.LayoutParams) tmp.getLayoutParams();
                layoutParams.x = ((bd.getBitmap().getWidth()/2));
                layoutParams.y = ((bd.getBitmap().getHeight()/2));
                tmp.setLayoutParams( layoutParams );

               /*
                tmp.shifts[0] = ((bd.getBitmap().getWidth()/2));
                tmp.shifts[1] = ((bd.getBitmap().getHeight()/2));
                extObj.moveMarker(tmp.imageView, tmp.coords[0], tmp.coords[1]);*/
            }
        }


    // XTileView members
    private Context context;
    private HashMap<String, View> markerList;
    //private XTileViewEventListener xListener;
    private HashMap<String, NavigablePath> paths;

    // XTileView methods
	// Constructor is similar to the original one, since it has the same argument.
	// It must be noticed that the management of the of zoomable marker is automatic since 
	// an 'xListener' is associated to the current 'XTileView' obj. No need to set a listener.
    public XTileView(Context context)
    {
        super(context);
        this.context = context;
        markerList = new HashMap<>();
       // xListener = new XTileViewEventListener();
        paths = new HashMap<>();
        //super.addTileViewEventListener(xListener);
    }

    private static String getZoomLevel(double scale)
    {
        String zoomLevel;
        if(scale <= ZOOM_3)
            zoomLevel = "0";
        else if(scale <= ZOOM_4)
            zoomLevel = "1";
        else if(scale <= ZOOM_5)
            zoomLevel = "2";
        else if(scale <= ZOOM_6)
            zoomLevel = "3";
        else if(scale <= ZOOM_7)
            zoomLevel = "4";
        else if(scale <= ZOOM_8)
            zoomLevel = "5";
        else
            zoomLevel = "6";

        return zoomLevel;
    }


    public void addCurrentMarker(View view, String imageBaseName, double x, double y)
    {
        arrow.put(imageBaseName,(ImageView) view);
        super.addMarker(view, x, y,-0.5f,-1f);
        forceZoom(getScale());
    }


    // For compatibility reasons: if markerKey is not set, it will be equal to 'imageBaseName'.
	public void addZoomableMarker(View view, String imageBaseName, double x, double y)
    {
        arrow.put(imageBaseName,(ImageView) view);
        super.addMarker(view, x, y,-0.5f,-1f);
        forceZoom(getScale());
    }

	public void addZoomableMarker(View view, String imageBaseName, String markerKey, double x, double y)
    {

       arrow.put(markerKey, (ImageView)view);
        super.addMarker(view, x, y,-0.5f,-1f);

        forceZoom(getScale());
    }

    public void removeZoomableMarker(String markerKey)
    {

        Toast.makeText(context," "+arrow.size(),Toast.LENGTH_SHORT).show();

        Log.e("Arrows", " "+arrow.size());

        if(!arrow.containsKey(markerKey))
            return;
        View tmp = arrow.remove(markerKey);
        super.removeMarker(tmp);
    }

	public void forceZoom(float scale)
    {
        super.setScale(scale);
        onScaleChanged(scale);
    }

    // For compatibility reasons (inefficient)
 /*   @Override
    public void moveMarker(View view, double x, double y)
    {
        View tmp = null;

        for(String key : markerList.keySet())
        {
            tmp = markerList.get(key);
            if(tmp.imageView == view)
                break;
        }

        if(tmp != null)
        {
            tmp.coords[0] = x;
            tmp.coords[1] = y;
            super.moveMarker(view, x - tmp.shifts[0], y - tmp.shifts[1]);
        }
    }

    public void moveMarker(String markerKey, double x, double y)
    {
        Marker tmp = markerList.get(markerKey);
        if(tmp != null)
        {
            tmp.coords[0] = x;
            tmp.coords[1] = y;
            super.moveMarker(tmp.imageView, x - tmp.shifts[0], y - tmp.shifts[1]);
        }
    }*/

    // It returns the number of path drawn on the current obj
	public int getPathsDrawn(){
        return paths.size();
    }

    // It returns an existing 'NavigablePath' which id is 'pathId'
	public NavigablePath getNavigablePath(String pathId){
        return paths.get(pathId);
    }

    // It computes the directions of the arrows along the path and draws them
	private void getPathDirections(NavigablePath navigablePath, String pathId)
    {

        //if(marker1!=null)super.removeView(marker1);
        ImageView imageView;
        String s;
        // relevance = 0 -> relevant movement along X
        // relevance = 1 -> relevant movement along Y
        int relevance;
        // direction = 0 -> direction on SX/UP
        // direction = 1 -> direction on DX/DOWN
        int direction, resId;
        double[] current, next, diff;
        double scale = getScale();
        diff = new double[2];

        for(int i=1; i<(navigablePath.points.size() - 1); i++)
        {
            imageView =  new ImageView(context);
            relevance = 1;
            direction = 1;
            // get next pair of point
            current = navigablePath.points.get(i);
            next = navigablePath.points.get(i + 1);
            // compute differences
            diff[0] = current[0] - next[0];
            diff[1] = current[1] - next[1];
            // evaluate the relevance
            if(Math.abs(diff[0]) > Math.abs(diff[1]))
                relevance = 0;
            // evaluate the direction
            if(diff[relevance] > 0)
                direction = 0;
            // compute directions
            // LEFT
            if(relevance == 0 && direction == 0)
                s = "arrow_left";
            // RIGHT
            else if(relevance == 0 && direction == 1)
                s = "arrow_right";
            // UP
            else if(relevance == 1 && direction == 0)
                s = "arrow_up";
            // DOWN
            else
                s = "arrow_down";

            resId = context.getResources().getIdentifier(s.toString()+"_" + XTileView.getZoomLevel(scale), "drawable", "com.findTools.Find");

            Log.d("stest",""+s.toString()+"_" + XTileView.getZoomLevel(scale));
            imageView.setImageResource(resId);

            marker1=imageView;
            addZoomableMarker(imageView, s, pathId + "_" + "arrow" + i, current[0], current[1]);

            arrow.put(pathId + "_" + "arrow" + i,imageView);

            Log.d("cur"," "+imageView.toString()+" "+current[0]+","+current[1]);

           // addZoomableMarker(imageView,"test",current[0],current[1]);
        }
    }

    public void drawNavigablePath(String pathId, NavigablePath navigablePath, int br)
    {
        int itemNum = navigablePath.points.size();
        double scale = getScale();

        // add the navigablePath to the HashMap containing all navigablePath obj
        paths.put(pathId, navigablePath);

        ImageView current=new ImageView(context);
        double[] currentcords=navigablePath.points.get(0);
        int cresId = context.getResources().getIdentifier("blue_dot_" + XTileView.getZoomLevel(scale),   "drawable", "com.findTools.Find");
        current.setImageResource(cresId);
        addZoomableMarker(current, "pin", currentcords[0], currentcords[1]);

        // add pin marker to destination
        ImageView destination = new ImageView(context);
        double[] destCords = navigablePath.points.get(itemNum - 1);

        Log.d("scale"," "+scale);





        int resId;
        if(br==1)
            resId = context.getResources().getIdentifier("stairs",   "drawable", "com.findTools.Find");

        else
        resId = context.getResources().getIdentifier("pin_" + XTileView.getZoomLevel(scale),   "drawable", "com.findTools.Find");




       // destination.setImageResource(R.drawable.pin_0);

        destination.setImageResource(resId);
        Log.d("dest"," "+destination.toString()+" "+destCords[0]+","+destCords[1]);

        addZoomableMarker(destination, "pind", destCords[0], destCords[1]);

        Log.d("asd","asd");

        // add marker for the other points in the path according the right direction
        getPathDirections(navigablePath, pathId);

        Log.d("q","asd");

        // draw again the blue dot due to visibility issues
        //if(markerList.containsKey("blue_dot"))  moveMarker("blue_dot", markerList.get("blue_dot").coords[0], markerList.get("blue_dot").coords[1]);

        Log.d("w","asd");
    }

    public void removeNavigablePath(String pathId)
    {

        // remove destination pin marker
        removeZoomableMarker("pin");
        removeZoomableMarker("pind");
        // remove arrow markers
        for(int i=0; i<(paths.get(pathId).points.size() - 1); i++)
            removeZoomableMarker(pathId + "_" + "arrow" + i);
        // remove NavigablePath pathId from the list of the available ones
        paths.remove(pathId);
    }
}