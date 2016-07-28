package rva.firemind.com.hazardwizard;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowCloseListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class HazardWizard extends AppCompatActivity implements
        LocationListener,
        OnInfoWindowClickListener,
        OnMapReadyCallback,
        OnInfoWindowLongClickListener,
        OnInfoWindowCloseListener {

    private static final LatLng THAYER_STOP_SIGN_A = new LatLng(41.830155, -71.400994);

    private static final LatLng THAYER_STOP_SIGN_B = new LatLng(41.825569, -71.400561);

    private static final LatLng THAYER_STOP_SIGN_C = new LatLng(41.825483, -71.400075);

    private static LatLng currentLocation;

    /** Demonstrates customizing the info window and/or its contents. */
    class CustomInfoWindowAdapter implements InfoWindowAdapter {

        // These a both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mWindow;

        private final View mContents;

        CustomInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            if (mOptions.getCheckedRadioButtonId() != R.id.custom_info_window) {
                // This means that getInfoContents will be called.
                return null;
            }
            render(marker, mWindow);
            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            if (mOptions.getCheckedRadioButtonId() != R.id.custom_info_contents) {
                // This means that the default info contents will be used.
                return null;
            }
            render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {
            int badge;
            // Use the equals() method on a Marker to check for equals.  Do not use ==.
            if (marker.equals(mThayerStopA)) {
                badge = R.drawable.badge_stop;
            } else if(marker.equals(mThayerStopB)) {
                badge = R.drawable.badge_stop;
            } else if(marker.equals(mThayerStopC)) {
                badge = R.drawable.badge_stop;
            } else {
                // Passing 0 to setImageResource will clear the image view.
                badge = 0;
            }
            ((ImageView) view.findViewById(R.id.badge)).setImageResource(badge);

            String title = marker.getTitle();
            TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null) {
                // Spannable string allows us to edit the formatting of the text.
                SpannableString titleText = new SpannableString(title);
                titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
            if (snippet != null && snippet.length() > 12) {
                SpannableString snippetText = new SpannableString(snippet);
                snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 10, 0);
                snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12, snippet.length(), 0);
                snippetUi.setText(snippetText);
            } else {
                snippetUi.setText("");
            }
        }
    }

    private GoogleMap mMap;

    private Marker mThayerStopA;

    private Marker mThayerStopB;

    private Marker mThayerStopC;

    /**
     * Keeps track of the last selected marker (though it may no longer be selected).  This is
     * useful for refreshing the info window.
     */
    private Marker mLastSelectedMarker;

    private RadioGroup mOptions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        mOptions = (RadioGroup) findViewById(R.id.custom_info_window_options);
        mOptions.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mLastSelectedMarker != null && mLastSelectedMarker.isInfoWindowShown()) {
                    // Refresh the info window when the info window's content has changed.
                    mLastSelectedMarker.showInfoWindow();
                }
            }
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }

    @Override
    public void onMapReady(GoogleMap map) {


        mMap = map;

        // Hide the zoom controls as the button panel will cover it.
        mMap.getUiSettings().setZoomControlsEnabled(false);

        // Add lots of markers to the map.
        addMarkersToMap();

        // Setting an info window adapter allows us to change the both the contents and look of the
        // info window.
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Set listeners for marker events.  See the bottom of this class for their behavior.
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnInfoWindowCloseListener(this);
        mMap.setOnInfoWindowLongClickListener(this);


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 12);

        }

        /**
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);

        Location location = locationManager.getLastKnownLocation(bestProvider);

        if (location != null) {
            onLocationChanged(location);
        }

        locationManager.requestLocationUpdates(bestProvider, 20000, 0, this);
        */


        // Override the default content description on the view, for accessibility mode.
        // Ideally this string would be localised.
        mMap.setContentDescription("Map with locations of hazards");

        // Pan to see all markers in view.
        // Cannot zoom to bounds until the map has a size.
        final View mapView = getSupportFragmentManager().findFragmentById(R.id.map).getView();
        if (mapView.getViewTreeObserver().isAlive()) {
            mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation") // We use the new method when supported
                @SuppressLint("NewApi") // We check which build version we are using.
                @Override
                public void onGlobalLayout() {
                    LatLngBounds bounds = new LatLngBounds.Builder()
                            .include(THAYER_STOP_SIGN_A)
                            .include(THAYER_STOP_SIGN_B)
                            .include(THAYER_STOP_SIGN_C)
                            .build();
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 12: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    finish();
                }

                break;
            }
        }
    }

    private void addMarkersToMap() {
        mThayerStopA = mMap.addMarker(new MarkerOptions()
                .position(THAYER_STOP_SIGN_A)
                .title("Stop Sign")
                .snippet("Stop")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.badge_stop))
                .draggable(false));

        mThayerStopB = mMap.addMarker(new MarkerOptions()
                .position(THAYER_STOP_SIGN_B)
                .title("Stop Sign")
                .snippet("Stop")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.badge_stop))
                .draggable(false));

        mThayerStopC = mMap.addMarker(new MarkerOptions()
                .position(THAYER_STOP_SIGN_C)
                .title("Stop Sign")
                .snippet("Stop")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.badge_stop))
                .draggable(false));
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .draggable(false));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    private boolean checkReady() {
        if (mMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /** Called when the Clear button is clicked. */
    public void onClearMap(View view) {
        if (!checkReady()) {
            return;
        }
        mMap.clear();
    }

    /** Called when the Reset button is clicked. */
    public void onResetMap(View view) {
        if (!checkReady()) {
            return;
        }
        // Clear the map because we don't want duplicates of the markers.
        mMap.clear();
        addMarkersToMap();
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }


    //
    // Marker related listeners.
    //


    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this, "Click Info Window", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        //Toast.makeText(this, "Close Info Window", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        Toast.makeText(this, "Info Window long click", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onProviderDisabled(String provider) {
        //u wot m8
    }
    @Override
    public void onProviderEnabled(String provider) {
        //u wot m9
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //u wot m80
        //m80 is also a pretty good rapperino
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

}
