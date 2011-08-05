package com.android.priceticker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XValueMarker;
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

public class GraphingActivity extends Activity implements OnTouchListener, OnClickListener {
	private XYPlot mySimpleXYPlot;
	
	private SimpleXYSeries callBidVsStrike;
	private SimpleXYSeries callAskVsStrike;
	private SimpleXYSeries putBidVsStrike;
	private SimpleXYSeries putAskVsStrike;
	private SimpleXYSeries deltaVsStrike;
	private SimpleXYSeries gammaVsStrike;
	private SimpleXYSeries thetaVsStrike;
	private SimpleXYSeries vegaVsStrike;
	private SimpleXYSeries rhoVsStrike;
	
	private PointF minXY;
	private PointF maxXY;
	private float absMinX;
	private float absMaxX;
	private float minNoError;
	private float maxNoError;
	private double minDif;
	
	private CheckBox checkbox1;
	private CheckBox checkbox2;
	private CheckBox checkbox3;
	private CheckBox checkbox4;
	private CheckBox checkbox5;
	private CheckBox checkbox6;
	private CheckBox checkbox7;
	private CheckBox checkbox8;
	private CheckBox checkbox9;
	
	// Storage for table data sent over from OptionsActivity
	private ArrayList<Double> putBidSeries;
    private ArrayList<Double> putAskSeries;
    private ArrayList<Double> strikeSeries;
    private ArrayList<Double> callBidSeries;
    private ArrayList<Double> callAskSeries;
    private ArrayList<Double> deltaSeries;
    private ArrayList<Double> gammaSeries;
    private ArrayList<Double> vegaSeries;
    private ArrayList<Double> thetaSeries;
    private ArrayList<Double> rhoSeries;
    private double futuresPrice;
    
    private static int seriesCount = 0;
    
	
	final private double difPadding = 0.0;
	 
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graphing);
        
        checkbox1 = (CheckBox) findViewById(R.id.checkbox1);
        checkbox2 = (CheckBox) findViewById(R.id.checkbox2);
        checkbox3 = (CheckBox) findViewById(R.id.checkbox3);
        checkbox4 = (CheckBox) findViewById(R.id.checkbox4);
        checkbox5 = (CheckBox) findViewById(R.id.checkbox5);
        checkbox6 = (CheckBox) findViewById(R.id.checkbox6);
        checkbox7 = (CheckBox) findViewById(R.id.checkbox7);
        checkbox8 = (CheckBox) findViewById(R.id.checkbox8);
        checkbox9 = (CheckBox) findViewById(R.id.checkbox9);
        
        checkbox1.setOnClickListener(this);
        checkbox2.setOnClickListener(this);
        checkbox3.setOnClickListener(this);
        checkbox4.setOnClickListener(this);
        checkbox5.setOnClickListener(this);
        checkbox6.setOnClickListener(this);
        checkbox7.setOnClickListener(this);
        checkbox8.setOnClickListener(this);
        checkbox9.setOnClickListener(this);
        
        checkbox1.setChecked(true);
        checkbox2.setChecked(true);
        checkbox3.setChecked(true);
        checkbox4.setChecked(true);
        
        // Retrieve and unpackage graphing data sent from Options activity
        HashMap<String, ArrayList<Double>> serializableExtra = (HashMap<String, ArrayList<Double>>) getIntent().getSerializableExtra("pt");
		HashMap<String, ArrayList<Double>> hm = serializableExtra;
		putBidSeries = hm.get("bidPut");
		putAskSeries = hm.get("askPut");
	    strikeSeries = hm.get("strikePut");
	    callBidSeries = hm.get("bidCall");
	    callAskSeries = hm.get("askCall");
	    deltaSeries = hm.get("deltaCall");
	    gammaSeries = hm.get("gammaCall");
	    vegaSeries = hm.get("vegaCall");
	    thetaSeries = hm.get("thetaCall");
	    rhoSeries = hm.get("rhoCall");
	    futuresPrice = getIntent().getExtras().getDouble("futuresPrice");
	    
        // Initialize our XYPlot reference:
        mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        mySimpleXYPlot.setOnTouchListener(this);
        mySimpleXYPlot.disableAllMarkup();
        
 
        // Turn the above ArrayLists into XYSeries:
        callBidVsStrike = new SimpleXYSeries(strikeSeries, callBidSeries, "Call Bid");
        callAskVsStrike = new SimpleXYSeries(strikeSeries, callAskSeries, "Call Ask");
        putAskVsStrike = new SimpleXYSeries(strikeSeries, putAskSeries, "Put Ask");
        putBidVsStrike = new SimpleXYSeries(strikeSeries, putBidSeries, "Put Bid");
        deltaVsStrike = new SimpleXYSeries(strikeSeries, deltaSeries, "Delta");
        gammaVsStrike = new SimpleXYSeries(strikeSeries, gammaSeries, "Gamma");
        vegaVsStrike = new SimpleXYSeries(strikeSeries, vegaSeries, "Vega");
        thetaVsStrike = new SimpleXYSeries(strikeSeries, thetaSeries, "Theta");
        rhoVsStrike = new SimpleXYSeries(strikeSeries, rhoSeries, "Rho");

        // Display 4 checkboxes by default
        addSeries(checkbox1);
        addSeries(checkbox2);
        addSeries(checkbox3);
        addSeries(checkbox4);

        XValueMarker futuresMarker = new XValueMarker(futuresPrice, "Current Future Price");
        Paint greenPaint = new Paint();
        greenPaint.setColor(Color.GREEN);
        Paint blackPaint = new Paint();
        blackPaint.setColor(Color.BLACK);
        futuresMarker.setLinePaint(greenPaint);
        futuresMarker.setTextPaint(blackPaint);

        mySimpleXYPlot.addMarker(futuresMarker);
        
        mySimpleXYPlot.setDomainLabel("Strike Price ($)");
        
        //Enact all changes
		mySimpleXYPlot.redraw();

    }
    

	@Override
	public void onClick(View v) {
        if (((CheckBox) v).isChecked()) {
            addSeries((CheckBox) v);
        } else {
            removeSeries((CheckBox) v);
        }
    }
    
	private void addSeries(CheckBox v) {
		String whichValue = v.getText().toString();
		
		if (whichValue.equals("Call Bid"))
			mySimpleXYPlot.addSeries(callBidVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Call Ask"))
			mySimpleXYPlot.addSeries(callAskVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Put Bid"))
			mySimpleXYPlot.addSeries(putBidVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Put Ask"))
			mySimpleXYPlot.addSeries(putAskVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Delta"))
			mySimpleXYPlot.addSeries(deltaVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Gamma"))
			mySimpleXYPlot.addSeries(gammaVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Vega"))
			mySimpleXYPlot.addSeries(vegaVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Theta"))
			mySimpleXYPlot.addSeries(thetaVsStrike, myLPFormatter(Color.RED));
		if (whichValue.equals("Rho"))
			mySimpleXYPlot.addSeries(rhoVsStrike, myLPFormatter(Color.RED));
		
		seriesCount++;
		
		// Recalculate min/max values
		calculateTouchValues();

		//Enact all changes
		mySimpleXYPlot.redraw();
		
	}
	
	private void removeSeries(CheckBox v) {
		String whichValue = v.getText().toString();
		
		if (seriesCount > 1) {
			if (whichValue.equals("Call Bid"))
				mySimpleXYPlot.removeSeries(callBidVsStrike);
			if (whichValue.equals("Call Ask"))
				mySimpleXYPlot.removeSeries(callAskVsStrike);
			if (whichValue.equals("Put Bid"))
				mySimpleXYPlot.removeSeries(putBidVsStrike);
			if (whichValue.equals("Put Ask"))
				mySimpleXYPlot.removeSeries(putAskVsStrike);
			if (whichValue.equals("Delta"))
				mySimpleXYPlot.removeSeries(deltaVsStrike);
			if (whichValue.equals("Gamma"))
				mySimpleXYPlot.removeSeries(gammaVsStrike);
			if (whichValue.equals("Vega"))
				mySimpleXYPlot.removeSeries(vegaVsStrike);
			if (whichValue.equals("Theta"))
				mySimpleXYPlot.removeSeries(thetaVsStrike);
			if (whichValue.equals("Rho"))
				mySimpleXYPlot.removeSeries(rhoVsStrike);
			seriesCount--;
		}
		// Don't allow user to remove final series
		// Note: Added due to bug in AndroidPlot that throws NPE if last series is removed
		else {
			v.toggle();
		}
		
		// Recalculate min/max values
		calculateTouchValues();

		//Enact all changes
		mySimpleXYPlot.redraw();
	}
	
	public void calculateTouchValues() {
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
				if (distBetweenFingers > 5f)
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
    
    public LineAndPointFormatter myLPFormatter(int c) {
    	
        LineAndPointFormatter lpFormatter = new LineAndPointFormatter(c, c, c);
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
            	Intent myIntent = new Intent(GraphingActivity.this, HelpActivity.class);
            	GraphingActivity.this.startActivity(myIntent);
            case R.id.exitIcon: 
            	java.lang.System.exit(0);                             
        }
        return true;
    }


}