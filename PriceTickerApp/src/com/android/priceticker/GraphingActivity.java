package com.android.priceticker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

/****************************************************
 * Price Ticker App - Graphing Activity
 * 
 * @author Ben Siver
 * @date 6/28/2011
 * 
 * This activity is launched from the OptionsActivity when a user selects the
 * Graphing choice from the context menu.  Various data from the OptionsActivity's
 * PriceTick array is bundled into a HashMap and sent over to the GraphingActivity.
 * This activity unpackages this data and creates numerous ArrayLists containing
 * the raw numerical data.  AndroidPlot is used to create series plots as well as
 * other graphing features.
 *
 *
 *****************************************************
 */

public class GraphingActivity extends Activity implements OnTouchListener {
	private XYPlot mySimpleXYPlot;
	private Random random;
	
	private SimpleXYSeries callBidVsStrike;
	private SimpleXYSeries callAskVsStrike;
	private SimpleXYSeries putBidVsStrike;
	private SimpleXYSeries putAskVsStrike;
	private PointF minXY;
	private PointF maxXY;
	private float absMinX;
	private float absMaxX;
	private float minNoError;
	private float maxNoError;
	private double minDif;
	
	final private double difPadding = 0.0;
	 
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graphing);
                
        // Retrieve and unpackage graphing data sent from Options activity
        HashMap<String, ArrayList<Double>> serializableExtra = (HashMap<String, ArrayList<Double>>) getIntent().getSerializableExtra("pt");
		HashMap<String, ArrayList<Double>> hm = serializableExtra;
        ArrayList<Double> putBidSeries = hm.get("bidPut");
        ArrayList<Double> putAskSeries = hm.get("askPut");
        ArrayList<Double> putStrikeSeries = hm.get("strikePut");
        
        ArrayList<Double> callBidSeries = hm.get("bidCall");
        ArrayList<Double> callAskSeries = hm.get("askCall");
        ArrayList<Double> callStrikeSeries = hm.get("strikeCall");
        
        // Initialize our XYPlot reference:
        mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        mySimpleXYPlot.setOnTouchListener(this);
        mySimpleXYPlot.disableAllMarkup();
        
 
        // Turn the above ArrayLists into XYSeries:
       callBidVsStrike = new SimpleXYSeries(
                callStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
                callBidSeries, // Y_VALS_ONLY means use the element index as the x value
                "Call Bid");                             // Set the display title of the series
       
      callAskVsStrike = new SimpleXYSeries(
               callStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
               callAskSeries, // Y_VALS_ONLY means use the element index as the x value
               "Call Ask");                             // Set the display title of the series
       
       putAskVsStrike = new SimpleXYSeries(
               putStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
               putAskSeries, // Y_VALS_ONLY means use the element index as the x value
               "Put Ask");                             // Set the display title of the series
       
       putBidVsStrike = new SimpleXYSeries(
               putStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
               putBidSeries, // Y_VALS_ONLY means use the element index as the x value
               "Put Bid");   
             
        // Add series1 to the xyplot:
        mySimpleXYPlot.addSeries(callBidVsStrike, randomLPFormatter2());
        mySimpleXYPlot.addSeries(putBidVsStrike, randomLPFormatter());
        mySimpleXYPlot.addSeries(callAskVsStrike, randomLPFormatter2());
        mySimpleXYPlot.addSeries(putAskVsStrike, randomLPFormatter());
        
        mySimpleXYPlot.setDomainLabel("Strike Price ($)");
        mySimpleXYPlot.setRangeLabel("Bid/Ask Price ($)");
        
        //Enact all changes
		mySimpleXYPlot.redraw();
		
		//Set of internal variables for keeping track of the boundaries
		mySimpleXYPlot.calculateMinMaxVals();
		minXY = new PointF(mySimpleXYPlot.getCalculatedMinX().floatValue(),
				mySimpleXYPlot.getCalculatedMinY().floatValue()); //initial minimum data point
		absMinX = minXY.x; //absolute minimum data point
		//absolute minimum value for the domain boundary maximum
		minNoError = Math.round(callBidVsStrike.getX(1).floatValue() + 2);
		maxXY = new PointF(mySimpleXYPlot.getCalculatedMaxX().floatValue(),
				mySimpleXYPlot.getCalculatedMaxY().floatValue()); //initial maximum data point
		absMaxX = maxXY.x; //absolute maximum data point
		//absolute maximum value for the domain boundary minimum
		maxNoError = (float) Math.round(callBidVsStrike.getX(callBidVsStrike.size() - 1).floatValue()) - 2;
		
		//Check x data to find the minimum difference between two neighboring domain values
		//Will use to prevent zooming further in than this distance
		double temp1 = callBidVsStrike.getX(0).doubleValue();
		double temp2 = callBidVsStrike.getX(1).doubleValue();
		double temp3;
		double thisDif;
		minDif = 100;	//increase if necessary for domain values
		for (int i = 2; i < callBidVsStrike.size(); i++) {
			temp3 = callBidVsStrike.getX(i).doubleValue();
			thisDif = Math.abs(temp1 - temp3);
			if (thisDif < minDif)
				minDif = thisDif;
			temp1 = temp2;
			temp2 = temp3;
		}
		minDif = minDif + difPadding; //with padding, the minimum difference
    }
    
    
 // Definition of the touch states
	static final private int NONE = 0;
	static final private int ONE_FINGER_DRAG = 1;
	static final private int TWO_FINGERS_DRAG = 2;
	private int mode = NONE;
 
	private PointF firstFinger;
	private float lastScrolling;
	private float distBetweenFingers;
	private float lastZooming;
 
	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		switch(event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: // Start gesture
				firstFinger = new PointF(event.getX(), event.getY());
				mode = ONE_FINGER_DRAG;
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				//When the gesture ends, a thread is created to give inertia to the scrolling and zoom 
				final Timer t = new Timer();
				t.schedule(new TimerTask() {
					@Override
					public void run() {
						while(Math.abs(lastScrolling) > 1f || Math.abs(lastZooming - 1) < 1.01) {
							lastScrolling *= .8;	//speed of scrolling damping
							scroll(lastScrolling);
							lastZooming += (1 - lastZooming) * .2;	//speed of zooming damping
							zoom(lastZooming);
							checkBoundaries();
							try {
								mySimpleXYPlot.postRedraw();
							} catch (final InterruptedException e) {
								e.printStackTrace();
							}
							// the thread lives until the scrolling and zooming are imperceptible
						}
					}
				}, 0);
 
			case MotionEvent.ACTION_POINTER_DOWN: // second finger
				distBetweenFingers = spacing(event);
				// the distance check is done to avoid false alarms
				//if (distBetweenFingers > 5f)
					mode = TWO_FINGERS_DRAG;
				break;
			case MotionEvent.ACTION_MOVE:
				if (mode == ONE_FINGER_DRAG) {
					final PointF oldFirstFinger = firstFinger;
					firstFinger = new PointF(event.getX(), event.getY());
					lastScrolling = oldFirstFinger.x - firstFinger.x;
					scroll(lastScrolling);
					lastZooming = (firstFinger.y - oldFirstFinger.y) / mySimpleXYPlot.getHeight();
					if (lastZooming < 0)
						lastZooming = 1 / (1 - lastZooming);
					else
						lastZooming += 1;
					zoom(lastZooming);
					checkBoundaries();
					mySimpleXYPlot.redraw();
 
				} else if (mode == TWO_FINGERS_DRAG) {
					final float oldDist = distBetweenFingers;
					distBetweenFingers = spacing(event);
					lastZooming = oldDist / distBetweenFingers;
					zoom(lastZooming);
					checkBoundaries();
					mySimpleXYPlot.redraw();
				}
				break;
		}
		return true;
	}
 
	private void zoom(float scale) {
		final float domainSpan = maxXY.x - minXY.x;
		final float domainMidPoint = maxXY.x - domainSpan / 2.0f;
		final float offset = domainSpan * scale / 2.0f;
		minXY.x = domainMidPoint - offset;
		maxXY.x = domainMidPoint + offset;
	}
 
	private void scroll(float pan) {
		final float domainSpan = maxXY.x - minXY.x;
		final float step = domainSpan / mySimpleXYPlot.getWidth();
		final float offset = pan * step;
		minXY.x += offset;
		maxXY.x += offset;
	}
 
	private float spacing(MotionEvent event) {
		final float x = event.getX(0);// - event.getX(1);
		final float y = event.getY(0);// - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}
 
	private void checkBoundaries() {
		//Make sure the proposed domain boundaries will not cause plotting issues
		if (minXY.x < absMinX)
			minXY.x = absMinX;
		else if (minXY.x > maxNoError)
			minXY.x = maxNoError;
		if (maxXY.x > absMaxX)
			maxXY.x = absMaxX;
		else if (maxXY.x < minNoError)
			maxXY.x = minNoError;
		if (maxXY.x - minXY.x < minDif)
			maxXY.x = maxXY.x + (float) (minDif - (maxXY.x - minXY.x));
		mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.AUTO);
	}
    
    public LineAndPointFormatter randomLPFormatter() {
    	
        LineAndPointFormatter lpFormatter = new LineAndPointFormatter(
        		
                Color.RED, Color.RED, Color.RED);
        lpFormatter.setFillPaint(null);
        return lpFormatter;
    }
    
 public LineAndPointFormatter randomLPFormatter2() {
    	
        LineAndPointFormatter lpFormatter = new LineAndPointFormatter(
        		
                Color.BLACK, Color.BLACK, Color.BLACK);
        lpFormatter.setFillPaint(null);
        return lpFormatter;
    }
    

	
	// Called when user clicks menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return true;
    }

    // Menu handler
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.graphingIcon:  
            	Toast.makeText(this, "Already in Graphing View!", Toast.LENGTH_SHORT).show();   
                break;
            case R.id.helpIcon: 
            	Toast.makeText(this, "Launch Help Activity", Toast.LENGTH_SHORT).show();
                break;
            case R.id.exitIcon: 
            	java.lang.System.exit(0);                             
        }
        return true;
    }

}