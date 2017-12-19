package com.find.wifitool;

import android.content.Intent;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.qozix.tileview.TileView;

import java.util.ArrayList;

public class Navigate extends AppCompatActivity {

    private ArrayList<double[]> points = new ArrayList<>();

    {
        points.add( new double[] {400, 1400} );
        points.add( new double[] {650, 1400} );}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(Navigate.this,MapsActivity.class));

        TileView tileView = new TileView( this );
        tileView.setBackgroundColor( 0xFFe7e7e7 );

        tileView.setSize( 3309, 2339 );  // the original size of the untiled image
        tileView.addDetailLevel( 1f, "plan/0/1000/%d_%d.jpg");
        tileView.addDetailLevel( 0.5f, "plan/0/500/%d_%d.jpg");
        tileView.addDetailLevel( 0.25f, "plan/0/250/%d_%d.jpg");
        tileView.addDetailLevel( 0.125f, "plan/0/125/%d_%d.jpg");

            ImageView marker = new ImageView( this );
            // save the coordinate for centering and callout positioning
            // give it a standard marker icon - this indicator points down and is centered, so we'll use appropriate anchors
            marker.setImageResource(  R.drawable.map_marker_normal );
            // on tap show further information about the area indicated
            // this could be done using a OnClickListener, which is a little more "snappy", since
            // MarkerTapListener uses GestureDetector.onSingleTapConfirmed, which has a delay of 300ms to
            // confirm it's not the start of a double-tap. But this would consume the touch event and
            // interrupt dragging
         //   tileView.getMarkerLayout().setMarkerTapListener( markerTapListener );
            // add it to the view tree
            tileView.addMarker( marker, 400, 1400, -0.5f, -1.0f );

        ImageView marker1 = new ImageView( this );
        // save the coordinate for centering and callout positioning
        // give it a standard marker icon - this indicator points down and is centered, so we'll use appropriate anchors
        marker1.setImageResource(  R.drawable.map_marker_normal );
        tileView.addMarker( marker1, 650, 1400, -0.5f, -1.0f );


        // get metrics for programmatic DP
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        // get the default paint and style it.  the same effect could be achieved by passing a custom Paint instnace
        Paint paint = tileView.getDefaultPathPaint();
        // dress up the path effects and draw it between some points
        paint.setShadowLayer(
                TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 4, metrics ),
                TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2, metrics ),
                TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2, metrics ),
                0x66000000
        );
        paint.setStrokeWidth( TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 5, metrics ) );
        paint.setPathEffect(
                new CornerPathEffect(
                        TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 5, metrics )
                )
        );
        tileView.drawPath( points, null );




        tileView.setScaleLimits( 0, 2 );

        // start small and allow zoom
        tileView.setScale( 0.5f );

        // with padding, we might be fast enough to create the illusion of a seamless image
        tileView.setViewportPadding( 256 );

        // we're running from assets, should be fairly fast decodes, go ahead and render asap
        tileView.setShouldRenderWhilePanning( true );

        setContentView(tileView);

    }

}
