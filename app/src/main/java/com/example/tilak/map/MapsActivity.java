package com.example.tilak.map;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    ArrayList markerPoints = new ArrayList();
    ArrayList path = new ArrayList();
    LocationManager mLocationManager;
    final int LOCATION_REFRESH_TIME = 10000;
    final int LOCATION_REFRESH_DISTANCE = 1000;

    private LocationManager locationManager;
    SupportMapFragment mapFragment;
    private String provider;
    private double latitude, longitude;
    Location location;
    FloatingActionButton fabLocate;
    private boolean canGetLocation;
    Marker ourGlobalMarker;
    ArrayList stepsArray= new ArrayList();
    TextView distanceTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        distanceTextView= findViewById(R.id.distanceTextView);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mLocationListener);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
       mapFragment= (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fabLocate=findViewById(R.id.fabLocate);
        fabLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapsActivity.this, AlertDialogActivity.class);
                i.putExtra("Route", stepsArray);
                startActivity(i);
            }
        });

        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the locatioin provider -> use
        // default
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location location = getLastKnownLocation();


        // Initialize the location fields
        if (location != null) {
            Log.i("Provider","Provider " + provider + " has been selected.");
        } else {
            Toast.makeText(this, "Cannot find your location", Toast.LENGTH_LONG).show();
        }
    }


    private void getCurrentLocation(){
        if(location!=null && !markerPoints.isEmpty() && location.getLatitude()!=((LatLng) markerPoints.get(0)).latitude && location.getLongitude()!=((LatLng) markerPoints.get(0)).longitude  && (markerPoints.size()==1||!markerPoints.get(0).equals(markerPoints.get(1)))) {
            Log.d("if","if called");
            LatLng sydney = new LatLng(latitude,longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 18));
            MarkerOptions markerOptions= new MarkerOptions();
            markerOptions.position(sydney);
            if(markerPoints.size()==1) {
                markerPoints.add(1, markerPoints.get(0));
                markerPoints.add(0, sydney);
            }
            else {
                markerPoints.add(sydney);
            }
            CircleOptions options=new CircleOptions();
            options.center(sydney);
            options.radius(20);
            options.fillColor(R.color.Radius);
            options.strokeWidth((float) 0.1);
            mMap.addCircle(options);
            mMap.addMarker(markerOptions);
            if (markerPoints.size()>=2) {
                LatLng destination = (LatLng) markerPoints.get(1);
                mMap.clear();
                markerPoints.clear();

                sydney = new LatLng(latitude,longitude);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 18));
                markerOptions= new MarkerOptions();
                markerOptions.position(sydney);
                markerPoints.add(sydney);
                options=new CircleOptions();
                options.center(sydney);
                options.radius(20);
                options.fillColor(R.color.Radius);
                options.strokeWidth((float) 0.1);
                mMap.addCircle(options);
                mMap.addMarker(markerOptions);
                markerPoints.add(destination);
                MarkerOptions destinyMarker = new MarkerOptions();
                destinyMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                destinyMarker.position(destination);
                mMap.addMarker(destinyMarker);
            }
            // Checks, whether start and end locations are captured
            if (markerPoints.size() >= 2) {
                LatLng origin = (LatLng) markerPoints.get(0);
                LatLng dest = (LatLng) markerPoints.get(1);

                // Getting URL to the Google Directions API
                String url = getDirectionsUrl(origin, dest);

                Log.d("URL", String.valueOf(url));
                DownloadTask downloadTask = new DownloadTask();

                // Start downloading json data from Google Directions API
                downloadTask.execute(url);

            }
            Log.d("Marker", String.valueOf(markerPoints.get(0)));
            Log.d("Marker", String.valueOf(markerPoints.get(1)));
            Log.i("LocateMe", "called the method");
        }
        else if(markerPoints.size()==2 && markerPoints.get(0)==markerPoints.get(1)){
            markerPoints.remove(1);
            mMap.clear();
            MarkerOptions markCur=new MarkerOptions();
            markCur.position((LatLng)markerPoints.get(0));
            mMap.addMarker(markCur);
            Log.i("LocateMe", "called else if");

        }

        else {
            Toast.makeText(MapsActivity.this, "You are here!!", Toast.LENGTH_SHORT).show();
            LatLng sydney = new LatLng(latitude,longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((LatLng) sydney, 18));
        }
    }


   /* public double GetDistanceFromLatLon(double lat1, double lon1, double lat2, double lon2)
    {
        final int R = 6371;
        // Radius of the earth in km
        double dLat = deg2rad(lat2 - lat1);
        // deg2rad below
        double dLon = deg2rad(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        // Distance in meters
        return d*1000;
    }
    private double deg2rad(double deg)
    {
        return deg * (Math.PI / 180);
    }*/


    private Location getLastKnownLocation() {
        try {
            locationManager = (LocationManager) this
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled

            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            LOCATION_REFRESH_TIME,
                            LOCATION_REFRESH_DISTANCE, this);
                    Log.d("Network", "Network Enabled");
                    if (locationManager != null) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                        }
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                LOCATION_REFRESH_TIME,
                                LOCATION_REFRESH_DISTANCE, this);
                        Log.d("GPS", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    Log.d("Longitude", String.valueOf(location.getLongitude()));
        return location;
    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);
       // mapFragment.getMapAsync(this);
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
     //   locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if(ourGlobalMarker == null) { // First time adding marker to map
            ourGlobalMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            MarkerAnimation.animateMarkerToICS(ourGlobalMarker, latLng, new LatLngInterpolator.Spherical());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
            if(markerPoints.size()==2 &&latLng.equals(markerPoints.get(1))){
                Toast.makeText(MapsActivity.this,"You have reached your destination!!",Toast.LENGTH_LONG).show();
            }
            if(!path.isEmpty() && latLng.equals(((Route)path.get(0)).getEnd())){
                stepsArray.remove(0);
            }
        } else {
            MarkerAnimation.animateMarkerToICS(ourGlobalMarker, latLng, new LatLngInterpolator.Spherical());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        }
   //     markerPoints.add(latLng);
      /*  if (markerPoints.size() == 3) {
            LatLng origin = (LatLng) markerPoints.get(2);
            LatLng dest = (LatLng) markerPoints.get(1);

            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);

            Log.d("URL", String.valueOf(url));
            DownloadTask downloadTask = new DownloadTask();

            // Start downloading json data from Google Directions API
            downloadTask.execute(url);

        }*/

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        LatLng sydney = new LatLng(latitude,longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 18));
        MarkerOptions markerOptions= new MarkerOptions();
        markerOptions.position(sydney);
      //  mMap.addMarker(markerOptions);
        markerPoints.add(sydney);
        CircleOptions options=new CircleOptions();
        options.center(sydney);
        options.radius(20);
        options.fillColor(R.color.Radius);
        options.strokeWidth((float) 0.1);
        mMap.addCircle(options);


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (markerPoints.size() == 2) {
                    markerPoints.clear();
                    mMap.clear();
                }

                // Adding new item to the ArrayList
                markerPoints.add(latLng);

                // Creating MarkerOptions
                MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(latLng);

                if (markerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                }

                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options);

                // Checks, whether start and end locations are captured
                if (markerPoints.size() >= 2) {
                    LatLng origin = (LatLng) markerPoints.get(0);
                    LatLng dest = (LatLng) markerPoints.get(1);
                    mMap.addMarker(new MarkerOptions().position((LatLng) markerPoints.get(0)));

                    // Getting URL to the Google Directions API
                    String url = getDirectionsUrl(origin, dest);

                    Log.d("URL", String.valueOf(url));
                    DownloadTask downloadTask = new DownloadTask();

                    // Start downloading json data from Google Directions API
                    downloadTask.execute(url);

                }

            }
        });

    }


    @Override
    public void onStatusChanged(String provide, int status, Bundle extras) {

        provider=provide;
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
          /*  if(true)
                return;
            LatLng current=new LatLng(location.getLatitude(),location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16));
            MarkerOptions markerOptions= new MarkerOptions();
            markerOptions.position(current);
            markerPoints.add(0,current);
            mMap.clear();
            mMap.addMarker(markerOptions);
            mMap.addMarker(new MarkerOptions().position((LatLng) markerPoints.get(1)));
            //onMapReady(mMap);
            // Checks, whether start and end locations are captured
            if (markerPoints.size() >= 2) {
                LatLng origin = (LatLng) markerPoints.get(0);
                LatLng dest = (LatLng) markerPoints.get(1);

                // Getting URL to the Google Directions API
                String url = getDirectionsUrl(origin, dest);

                Log.d("URL", String.valueOf(url));
                DownloadTask downloadTask = new DownloadTask();

                // Start downloading json data from Google Directions API
                downloadTask.execute(url);

            }*/
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
    };



    @Override
    public void onProviderDisabled(String provider) {

    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
            TurnParser turnTask = new TurnParser();
            turnTask.execute(result);

        }
    }


    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points;
            PolylineOptions lineOptions = null;
                if (result==null){
                    Toast.makeText(MapsActivity.this, "You can't drive till there!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList();
                    lineOptions = new PolylineOptions();

                    List<HashMap<String, String>> path = result.get(i);

                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    lineOptions.addAll(points);
                    lineOptions.width(12);
                    lineOptions.color(Color.GREEN);
                    lineOptions.geodesic(true);

                }


// Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    private class TurnParser extends AsyncTask<String,Integer,List>{


        @Override
        protected List doInBackground(String... strings) {

            JSONObject object=null;
            try {
                object=new JSONObject(strings[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        int i=0;

            try {
                JSONArray routes=object.getJSONArray("routes");
                if (routes==null){
                    return null;
                }
                JSONObject routeMember  =routes.getJSONObject(0);
                JSONArray legs= routeMember.getJSONArray("legs");
                JSONObject legMember= legs.getJSONObject(0);
                JSONArray steps= legMember.getJSONArray("steps");
                path.clear();
                stepsArray.clear();
                for (i=0;i<steps.length();++i){
                    JSONObject currentStep=steps.getJSONObject(i);
                    int distance=currentStep.getJSONObject("distance").getInt("value");
                    String turns=(stripHtml(currentStep.getString("html_instructions")));
                    int duration=currentStep.getJSONObject("duration").getInt("value");
                    double startLat=currentStep.getJSONObject("start_location").getDouble("lat");
                    double startLong=currentStep.getJSONObject("start_location").getDouble("lng");
                    double endLat=currentStep.getJSONObject("end_location").getDouble("lat");
                    double endLong=currentStep.getJSONObject("end_location").getDouble("lng");
                    Route currentRoute=new Route(distance,turns,duration,startLat,startLong,endLat,endLong);
                    path.add(currentRoute);
                    stepsArray.add(turns);
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

            return stepsArray;
        }

        @Override
        protected void onPostExecute(List list) {
            super.onPostExecute(list);
            for(Object i:list){
                Log.d("Turns", i.toString());
            }
            distanceTextView.setText(String.valueOf(((Route)path.get(0)).getDistance()));
            distanceTextView.setVisibility(View.VISIBLE);
            buildAlert(list);

        }
    }
    public String stripHtml(String html) {
        return Html.fromHtml(html).toString();
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private void buildAlert(List direction){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
        builderSingle.setTitle("Select One Name:-");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice,direction);


        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                AlertDialog.Builder builderInner = new AlertDialog.Builder(MapsActivity.this);
                builderInner.setMessage(strName);
                builderInner.setTitle("Your Selected Item is");
                builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        dialog.dismiss();
                    }
                });
                builderInner.show();
            }
        });
        builderSingle.show();
    }


}