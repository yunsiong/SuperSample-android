package com.quickblox.supersamples.main.activities;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.quickblox.supersamples.R;
import com.quickblox.supersamples.sdk.definitions.ActionResultDelegate;
import com.quickblox.supersamples.sdk.definitions.QBQueries;
import com.quickblox.supersamples.sdk.definitions.QueryMethod;
import com.quickblox.supersamples.sdk.definitions.QBQueries.QBQueryType;
import com.quickblox.supersamples.sdk.definitions.ResponseHttpStatus;
import com.quickblox.supersamples.sdk.helpers.LocationsXMLHandler;
import com.quickblox.supersamples.sdk.helpers.Query;
import com.quickblox.supersamples.sdk.helpers.Store;
import com.quickblox.supersamples.sdk.objects.LocationsList;
import com.quickblox.supersamples.sdk.objects.RestResponse;

import android.R.integer;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MapViewActivity extends MapActivity implements
		ActionResultDelegate {

	private MapView mapView;
	private Button back;
	List<Address> addressList;
	MapController mapController;
	private Drawable marker;
	
	private TimerTask task;
	private Timer timer;
	// thread callback handler
	private Handler mHandler = new Handler();
	
	private static boolean TIMER_STARTED = false;
	
	public static final String EXT_ID_GEOUSER = String.valueOf(Store.getInstance().getCurrentUser().findChild("external-user-id").getText());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		initMapView();
		initMyLocation();

		back = (Button) findViewById(R.id.back);

		marker = getResources().getDrawable(R.drawable.marker);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(),
				marker.getIntrinsicHeight());

		startTimer();
		// A run of a timer for update of the geopoints for each user 
		//============================================================
		/*timer = new Timer();
		task = new TimerTask() {

			@Override
			public void run() {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						ShowAllUsers whereAreUsers = new ShowAllUsers(marker);
						mapView.getOverlays().add(whereAreUsers);
						Toast.makeText(getBaseContext(),
								"The geopoints was changed!",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		};
		// each 10 seconds to do
		timer.schedule(task, 0, 10000);*/
		//=============================================================
		
		// get a latitude and a longitude of the current user
		LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		LocationListener locListener = new LocationListener() {

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
			}

			// if a location of the device will be changed,
			// send the data on the server
			@Override
			public void onLocationChanged(Location location) {
				if (location != null) {
					Toast.makeText(
							getBaseContext(),
							"New location latitude [" + location.getLatitude()
									+ "] longitude [" + location.getLongitude()
									+ "]", Toast.LENGTH_LONG).show();

					String lat = Double.toString(location.getLatitude());
					String lng = Double.toString(location.getLongitude());
					
					// create entity for current user
					List<NameValuePair> formparamsGeoUser = new ArrayList<NameValuePair>();
					formparamsGeoUser.add(new BasicNameValuePair(
							"geo_data[user_id]", EXT_ID_GEOUSER));
					formparamsGeoUser.add(new BasicNameValuePair(
							"geo_data[status]", QBQueries.STATUS));
					formparamsGeoUser.add(new BasicNameValuePair(
							"geo_data[latitude]", lat));
					formparamsGeoUser.add(new BasicNameValuePair(
							"geo_data[longitude]", lng));

					Log.i("EXTERNAL USER ID = ", EXT_ID_GEOUSER);
					
					UrlEncodedFormEntity postEntityGeoDataUser = null;
					try {
						postEntityGeoDataUser = new UrlEncodedFormEntity(
								formparamsGeoUser, "UTF-8");
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
					//
					// make query
					Query.makeQueryAsync(QueryMethod.Post,
							QBQueries.SEND_GPS_DATA_QUERY,
							postEntityGeoDataUser, null, MapViewActivity.this,
							QBQueries.QBQueryType.QBQueryTypeSendGPSData);			
				}
			}
		};

		// registration of the LocationListener
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, // update the geodata after 5 minutes (300 000 ms)
				0, locListener);
	}

	private void initMapView() {
		mapView = (MapView) findViewById(R.id.mapview);
		mapController = mapView.getController();
		mapView.setSatellite(true);
		mapView.setBuiltInZoomControls(true);
	}

	private void initMyLocation() {
		final MyLocationOverlay whereAmI = new MyLocationOverlay(this, mapView);
		// to begin follow for the updates of the location
		whereAmI.enableMyLocation();
		whereAmI.enableCompass(); // it's no works in the emulator
		whereAmI.runOnFirstFix(new Runnable() {

			@Override
			public void run() {
				// Show current location and change a zoom
				mapController.setZoom(3);
				mapController.animateTo(whereAmI.getMyLocation());
			}
		});
		mapView.getOverlays().add(whereAmI);
	}

	@Override
	protected boolean isLocationDisplayed() {
		return true;

	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	// back to the Main Activity
	public void exit(View v) {
		Intent intent = new Intent();
		intent.setClass(this, StartActivity.class);

		startActivity(intent);
		finish();

	}
	
	class ShowAllUsers extends ItemizedOverlay<OverlayItem> {

		private List<OverlayItem> locations = new ArrayList<OverlayItem>();
		private Drawable marker;
		LocationsList locList = null;
		private PopupPanel panel=new PopupPanel(R.layout.pop_up);

		public ShowAllUsers(Drawable marker) {
			super(marker);

			this.marker = marker;

			try {

				/** Handling XML */
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();

				/** Send URL to parse XML Tags */
				URL sourceUrl = new URL(QBQueries.GET_ALL_LOCATIONS_QUERY);

				/** Create handler to handle XML Tags ( extends DefaultHandler ) */
				LocationsXMLHandler locXMLHandler = new LocationsXMLHandler();
				xr.setContentHandler(locXMLHandler);
				xr.parse(new InputSource(sourceUrl.openStream()));

			} catch (Exception e) {
				Log.e("XML Parsing Exception = ", e.getMessage());
			}

			/** Get result from LocationsXMLHandler locationsList Object */
			locList = LocationsXMLHandler.locList;

			for (int i = 0; i < locList.getUserID().size(); i++) {
				if (locList.getUserID().get(i).equals(EXT_ID_GEOUSER) == false) {
					try {
						int lat = (int) (Double.parseDouble(locList.getLat()
								.get(i)) * 1000000);
						int lng = (int) (Double.parseDouble(locList.getLng()
								.get(i)) * 1000000);

						// the geodata adding in to list of the locations
						GeoPoint p = new GeoPoint(lat, lng);
						locations.add(new OverlayItem(p, "", ""));

					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				} else
					continue;

				populate();
			}
		}

		// a shadow of the marker
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);
			boundCenterBottom(marker);
		}
		
		@Override
		protected OverlayItem createItem(int i) {
			return locations.get(i);
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return locations.size();
		}
		
		@Override
		protected boolean onTap(int i) {		
			OverlayItem item = getItem(i);
			GeoPoint geo = item.getPoint();
			Point pt = mapView.getProjection().toPixels(geo, null);

			View view = panel.getView();

			((TextView) view.findViewById(R.id.latitude)).setText(String
					.valueOf(geo.getLatitudeE6() / 1000000.0));
			((TextView) view.findViewById(R.id.longitude)).setText(String
					.valueOf(geo.getLongitudeE6() / 1000000.0));

			panel.show(pt.y*2>mapView.getHeight());
		
			return true;
		}
	}

	@Override
	public void completedWithResult(QBQueryType queryType, RestResponse response) {
		if (queryType == QBQueries.QBQueryType.QBQueryTypeSendGPSData) {
			if (response.getResponseStatus() == ResponseHttpStatus.ResponseHttpStatus201) {
				Toast.makeText(this,
						"The current location has been added to the database",
						Toast.LENGTH_LONG).show();
			} else
				Toast.makeText(
						this,
						"The current location HAS NOT BEEN ADDED to the database!",
						Toast.LENGTH_LONG).show();
		}

	}

	public void startTimer() {
		if (!TIMER_STARTED)
		{
			TIMER_STARTED = true;
			Log.i("TIMER", "timer is run");
			timer = new Timer();
			task = new TimerTask() {

				@Override
				public void run() {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							ShowAllUsers whereAreUsers = new ShowAllUsers(
									marker);
							mapView.getOverlays().add(whereAreUsers);
							Toast.makeText(getBaseContext(),
									"The geopoints was changed!",
									Toast.LENGTH_SHORT).show();
						}
					});
				}
			};
			// each 30 seconds to do
			timer.schedule(task, 0, 30000);
		}
	}
		
	@Override
	protected void onStop() {
		timer.cancel();
		super.onStop();
		Log.i("TIMER", "timer is stop");
	}
	
	@Override
	protected void onPause() {
		timer.cancel();
		TIMER_STARTED = false;
		super.onPause();
		Log.i("TIMER", "timer is on pause");
	}
	
	@Override
	protected void onResume() {
		startTimer();	
		super.onResume();
	}
		
	class PopupPanel {
	    View popup;
	    boolean isVisible=false;
	    
	    PopupPanel(int layout) {
	      ViewGroup parent=(ViewGroup)mapView.getParent();

	      popup=getLayoutInflater().inflate(layout, parent, false);
	                  
	      popup.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	          hide();
	        }
	      });
	    }
	    
	    View getView() {
	      return(popup);
	    }
	    
	    void show(boolean alignTop) {
	    	
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
	      
			if (alignTop) {
				lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				lp.setMargins(0, 20, 0, 0);
			} else {
				lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				lp.setMargins(0, 0, 0, 60);
			}

			hide();
	      
	      ((ViewGroup)mapView.getParent()).addView(popup, lp);
	      isVisible=true;
	    }
	    
	    void hide() {
	      if (isVisible) {
	        isVisible=false;
	        ((ViewGroup)popup.getParent()).removeView(popup);
	      }
	    }
	  }

}



