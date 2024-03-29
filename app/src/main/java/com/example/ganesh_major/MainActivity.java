package com.example.ganesh_major;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleMap.OnMapClickListener, PlaceSelectionListener {

    public static final String TAG = "PiyushTag";
    static MapFragment mapFragment;
    static FragmentManager fragmentManager;
    static FragmentTransaction fragmentTransaction;
    public RelativeLayout book_transport;
    GoogleMap googleMap;
    EditText e_getLocations;
    static Location location;
    static Location des_location;
    static MarkerOptions userMarker;
    static MarkerOptions des_marker;
    GeoDataClient geoDataClient;
    PlaceDetectionClient placeDetectionClient;
    public FusedLocationProviderClient fusedLocationProviderClient;
    int auto_Complete_Request = 11;
    static ProgressDialog progressDialog;
    static TextView destination_location;
    //List<Place.Field> fields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        destination_location = (TextView) findViewById(R.id.des_location_editText);
        //e_getLocations = (EditText) findViewById(R.id.edit_getLocation);

        //progressDialogue showing
        progressDialog = new ProgressDialog(MainActivity.this, AlertDialog.THEME_HOLO_DARK);
        progressDialog.setMessage("Fetching Resources");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.create();
        progressDialog.show();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        Log.i(TAG,"mapFragment INIT");
        new FetchLocation().execute();
        Log.i(TAG,"new FetchLocation().execute()");
        //fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);
        mapFragment.getMapAsync(MainActivity.this);
        book_transport = (RelativeLayout)findViewById(R.id.layout_book_now);
        //AutocompleteFilter filter = new AutocompleteFilter.Builder();
//        book_transport.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try{
//                    Intent i2 = new PlaceAutocomplete
//                            .IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
//                            .build(MainActivity.this);
//                    startActivityForResult(i2,auto_Complete_Request);
//                }
//                catch (Exception e){
//                    e.printStackTrace();
//                }
//
//
//            }
//        });

        destination_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    Intent i2 = new PlaceAutocomplete
                            .IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(MainActivity.this);
                    startActivityForResult(i2,auto_Complete_Request);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==auto_Complete_Request){
            if(resultCode==RESULT_OK){
                Place place = PlaceAutocomplete.getPlace(this,data);
                Log.i(TAG,"Place : "+place.getId()+","+place.getAddress()+","+place.getAttributions()
                        +","+place.getLatLng().latitude+","+place.getLatLng().longitude
                        +","+place.getLocale()+","+place.getName()+","+place.getPlaceTypes()+","+place.getPhoneNumber());
                putDestination_marker(place.getLatLng());
                destination_location.setText(place.getName());
                Toast.makeText(this,"Click or Drag Marker for Precise Location..",Toast.LENGTH_LONG).show();
////Creating Polyline------------
//                Polyline polyline = googleMap.addPolyline(new PolylineOptions()
//                        .clickable(false)
//                        .add(place.getLatLng(),
//                                new LatLng(location.getLatitude(),location.getLongitude())));
                setRouteOnMapPart01();
            }else if (resultCode==RESULT_CANCELED){
                Log.i(TAG,"RESULT_CANCELED");
                new fetchRoute(new LatLng(23.249330,77.471209),new LatLng(23.233333,77.434087)).execute();
                Log.i(TAG,"Background Route INIT");
            }else if(resultCode ==PlaceAutocomplete.RESULT_ERROR){
                Log.i(TAG,"RESULT_ERROR");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setRouteOnMapPart01(){
        progressDialog.setMessage("Getting Route Info..");
        progressDialog.show();
        new fetchRoute(new LatLng(location.getLatitude(),location.getLongitude()),new LatLng(des_location.getLatitude(),des_location.getLongitude())).execute();
    }
    public void setRouteOnMapPart02(ArrayList<LatLng> turnList){
        PolylineOptions polylineOptions = new PolylineOptions().clickable(false);
        for(LatLng l : turnList){
            polylineOptions.add(l);
        }
        Polyline polyline = googleMap.addPolyline(polylineOptions);
    }


    public void putDestination_marker(LatLng latlng){
        if(des_marker!=null&&des_marker.isVisible()){
            des_marker.position(latlng);
            des_location.setLatitude(latlng.latitude);
            des_location.setLongitude(latlng.longitude);
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
        }
        else {
            des_marker = new MarkerOptions();
            des_marker.visible(true).draggable(true).icon(BitmapDescriptorFactory.defaultMarker());
            googleMap.addMarker(des_marker.position(latlng));
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
            //googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(new LatLng(location.getLatitude(),location.getLongitude()),latlng),(int)getBaseContext().getResources().getDisplayMetrics().density*20));
            //googleMap.animateCamera(new CameraUpdateFactory.new LatLngBounds(new LatLngBounds(new LatLng(location.getLatitude(),location.getLongitude()),latlng),(int)10*getBaseContext().getResources().getDisplayMetrics().density));
        }
    }

    public void putMarker_user() {
        //Log.i(TAG, "putMarker");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.i(TAG,"My Location : "+location.getLatitude()+","+location.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16f));
        //this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        if (userMarker!=null&&userMarker.isVisible()) {
            userMarker.position(new LatLng(location.getLatitude(), location.getLongitude())).visible(true).draggable(true);
            //Log.i(TAG,"if marker status : "+userMarker.isVisible());
        } else {
            float scale = getBaseContext().getResources().getDisplayMetrics().density;
            Bitmap bm = BitmapFactory.decodeResource(getResources(),R.drawable.piyush_prof);
            Bitmap bm2 = Bitmap.createScaledBitmap(bm,30*(int)scale,30*(int)scale,false);
            userMarker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bm2));
            //userMarker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.piyush_prof)));
            googleMap.addMarker(userMarker.position(new LatLng(location.getLatitude(), location.getLongitude())).visible(true).draggable(true));
            //Log.i(TAG,"else marker status : "+userMarker.isVisible());
        }
        //Log.i(TAG,"setOnClickListener Applied..");
    }


    @Override
    public void onMapReady(GoogleMap gmap) {
        Log.i(TAG, "onMapReady");
        googleMap = gmap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //do nothing
    }

    @Override
    public void onPlaceSelected(Place place) {

    }

    @Override
    public void onError(Status status) {
        Log.i(TAG,"onError : status : "+status.toString());
    }

    public class FetchLocation extends AsyncTask<Void, Void, Void> {


        LocationRequest locationRequest;
        LocationCallback locationCallback;

        @Override
        protected Void doInBackground(Void... voids) {
            Log.i(TAG,"doInBackground");
            locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setSmallestDisplacement(10);
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getBaseContext());
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location l : locationResult.getLocations()) {
                        if (l == null) {
                            continue;
                        } else {
                            Log.i(TAG, "on Location Change");
                            location = l;
                            putMarker_user();
                            if (progressDialog.isShowing()){
                                progressDialog.dismiss();
                            }
                            fusedLocationProviderClient.removeLocationUpdates(this);
                            break;
                        }
                    }
                }
            };
            Log.i(TAG,"Location Callback Setup Done..");
            Looper looper = Looper.getMainLooper();
            if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, looper);
            Log.i(TAG,"asked for location");
            return null;
        }
    }

    class fetchRoute extends AsyncTask<Void,Void,Void>{

        public LatLng start_latlng;
        public LatLng end_latlng;
        //String final_base_url = "https://maps.googleapis.com/maps/api/directions/json?origin=23.249330,77.471209&destination=23.233333,77.434087&key=AIzaSyAswMLRUznUObWYUjMmbeWkodZS3vziRAE";
        String base_url_01 = "https://maps.googleapis.com/maps/api/directions/json?origin=";
        String base_url_02 = "&destination=";
        String base_url_03 = "&key=AIzaSyAswMLRUznUObWYUjMmbeWkodZS3vziRAE";

        String dataString="";
        ArrayList<LatLng> terminalList;

        public fetchRoute(LatLng start_latlng, LatLng end_latlng) {
            this.start_latlng = start_latlng;
            this.end_latlng = end_latlng;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String final_base_url =base_url_01+start_latlng.latitude+","+start_latlng.longitude+base_url_02+
                    end_latlng.latitude+","+end_latlng.longitude+base_url_03;
            terminalList = new ArrayList<LatLng>();
            URL url =null;
            HttpURLConnection conn=null;
            try{
                url = new URL(final_base_url);
                conn =(HttpURLConnection)url.openConnection();
                BufferedReader br =new BufferedReader(new InputStreamReader(conn.getInputStream()));
                for(String line;(line = br.readLine())!=null;){
                    dataString=dataString+line;
                    dataString=dataString+"\n";
                }
                dataString = "{\n" +
                        "   \"geocoded_waypoints\" : [\n" +
                        "      {\n" +
                        "         \"geocoder_status\" : \"OK\",\n" +
                        "         \"place_id\" : \"ChIJBbyFiZAJ1jsRpFAfAR6U1Fc\",\n" +
                        "         \"types\" : [ \"route\" ]\n" +
                        "      },\n" +
                        "      {\n" +
                        "         \"geocoder_status\" : \"OK\",\n" +
                        "         \"place_id\" : \"ChIJg_WQRJoJ1jsRKkQNE8nMeac\",\n" +
                        "         \"types\" : [ \"route\" ]\n" +
                        "      }\n" +
                        "   ],\n" +
                        "   \"routes\" : [\n" +
                        "      {\n" +
                        "         \"bounds\" : {\n" +
                        "            \"northeast\" : {\n" +
                        "               \"lat\" : 21.899992,\n" +
                        "               \"lng\" : 77.89439899999999\n" +
                        "            },\n" +
                        "            \"southwest\" : {\n" +
                        "               \"lat\" : 21.8983491,\n" +
                        "               \"lng\" : 77.8919708\n" +
                        "            }\n" +
                        "         },\n" +
                        "         \"copyrights\" : \"Map data ©2019 Google\",\n" +
                        "         \"legs\" : [\n" +
                        "            {\n" +
                        "               \"distance\" : {\n" +
                        "                  \"text\" : \"0.4 km\",\n" +
                        "                  \"value\" : 422\n" +
                        "               },\n" +
                        "               \"duration\" : {\n" +
                        "                  \"text\" : \"2 mins\",\n" +
                        "                  \"value\" : 100\n" +
                        "               },\n" +
                        "               \"end_address\" : \"Itarsi Rd, Sadar, Badora, Madhya Pradesh 460001, India\",\n" +
                        "               \"end_location\" : {\n" +
                        "                  \"lat\" : 21.899992,\n" +
                        "                  \"lng\" : 77.8919708\n" +
                        "               },\n" +
                        "               \"start_address\" : \"Genda Chowk Sadar Betul, Sadar, Badora, Madhya Pradesh 460001, India\",\n" +
                        "               \"start_location\" : {\n" +
                        "                  \"lat\" : 21.8991765,\n" +
                        "                  \"lng\" : 77.89437029999999\n" +
                        "               },\n" +
                        "               \"steps\" : [\n" +
                        "                  {\n" +
                        "                     \"distance\" : {\n" +
                        "                        \"text\" : \"60 m\",\n" +
                        "                        \"value\" : 60\n" +
                        "                     },\n" +
                        "                     \"duration\" : {\n" +
                        "                        \"text\" : \"1 min\",\n" +
                        "                        \"value\" : 11\n" +
                        "                     },\n" +
                        "                     \"end_location\" : {\n" +
                        "                        \"lat\" : 21.8986384,\n" +
                        "                        \"lng\" : 77.8943575\n" +
                        "                     },\n" +
                        "                     \"html_instructions\" : \"Head \\u003cb\\u003esouth\\u003c/b\\u003e toward \\u003cb\\u003eGenda Chowk Sadar Betul\\u003c/b\\u003e\",\n" +
                        "                     \"polyline\" : {\n" +
                        "                        \"points\" : \"{dtdCyvlzMV@Z@Z?ZA\"\n" +
                        "                     },\n" +
                        "                     \"start_location\" : {\n" +
                        "                        \"lat\" : 21.8991765,\n" +
                        "                        \"lng\" : 77.89437029999999\n" +
                        "                     },\n" +
                        "                     \"travel_mode\" : \"DRIVING\"\n" +
                        "                  },\n" +
                        "                  {\n" +
                        "                     \"distance\" : {\n" +
                        "                        \"text\" : \"0.2 km\",\n" +
                        "                        \"value\" : 183\n" +
                        "                     },\n" +
                        "                     \"duration\" : {\n" +
                        "                        \"text\" : \"1 min\",\n" +
                        "                        \"value\" : 59\n" +
                        "                     },\n" +
                        "                     \"end_location\" : {\n" +
                        "                        \"lat\" : 21.8985773,\n" +
                        "                        \"lng\" : 77.8928044\n" +
                        "                     },\n" +
                        "                     \"html_instructions\" : \"Turn \\u003cb\\u003eright\\u003c/b\\u003e at R S Dental Clinic onto \\u003cb\\u003eGenda Chowk Sadar Betul\\u003c/b\\u003e\\u003cdiv style=\\\"font-size:0.9em\\\"\\u003ePass by Maa Annpurna cycle Store (on the left)\\u003c/div\\u003e\",\n" +
                        "                     \"maneuver\" : \"turn-right\",\n" +
                        "                     \"polyline\" : {\n" +
                        "                        \"points\" : \"oatdCwvlzMJGRz@FVJ^BH@H?Z?TAJAFAHCFERINIHAFEP?JAJ\"\n" +
                        "                     },\n" +
                        "                     \"start_location\" : {\n" +
                        "                        \"lat\" : 21.8986384,\n" +
                        "                        \"lng\" : 77.8943575\n" +
                        "                     },\n" +
                        "                     \"travel_mode\" : \"DRIVING\"\n" +
                        "                  },\n" +
                        "                  {\n" +
                        "                     \"distance\" : {\n" +
                        "                        \"text\" : \"0.2 km\",\n" +
                        "                        \"value\" : 179\n" +
                        "                     },\n" +
                        "                     \"duration\" : {\n" +
                        "                        \"text\" : \"1 min\",\n" +
                        "                        \"value\" : 30\n" +
                        "                     },\n" +
                        "                     \"end_location\" : {\n" +
                        "                        \"lat\" : 21.899992,\n" +
                        "                        \"lng\" : 77.8919708\n" +
                        "                     },\n" +
                        "                     \"html_instructions\" : \"Turn \\u003cb\\u003eright\\u003c/b\\u003e at Verma Medical Agency onto \\u003cb\\u003eItarsi Rd\\u003c/b\\u003e\\u003cdiv style=\\\"font-size:0.9em\\\"\\u003ePass by Shri Krishna Automobiles-Betul (on the left)\\u003c/div\\u003e\\u003cdiv style=\\\"font-size:0.9em\\\"\\u003eDestination will be on the left\\u003c/div\\u003e\",\n" +
                        "                     \"maneuver\" : \"turn-right\",\n" +
                        "                     \"polyline\" : {\n" +
                        "                        \"points\" : \"catdC_mlzMYNoDfB}@`@QJ\"\n" +
                        "                     },\n" +
                        "                     \"start_location\" : {\n" +
                        "                        \"lat\" : 21.8985773,\n" +
                        "                        \"lng\" : 77.8928044\n" +
                        "                     },\n" +
                        "                     \"travel_mode\" : \"DRIVING\"\n" +
                        "                  }\n" +
                        "               ],\n" +
                        "               \"traffic_speed_entry\" : [],\n" +
                        "               \"via_waypoint\" : []\n" +
                        "            }\n" +
                        "         ],\n" +
                        "         \"overview_polyline\" : {\n" +
                        "            \"points\" : \"{dtdCyvlzMr@Bv@AJGRz@Rv@DR?p@Id@Ob@KPE\\\\AJYNmFhCQJ\"\n" +
                        "         },\n" +
                        "         \"summary\" : \"Genda Chowk Sadar Betul and Itarsi Rd\",\n" +
                        "         \"warnings\" : [],\n" +
                        "         \"waypoint_order\" : []\n" +
                        "      }\n" +
                        "   ],\n" +
                        "   \"status\" : \"OK\"\n" +
                        "}";
                Log.i(TAG,dataString);
                JSONObject jsonObject =new JSONObject(dataString);
                JSONArray jsonArray =jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
                for(int i=0;i<jsonArray.length();i++){
                    JSONObject jsonOBJ_temp =jsonArray.getJSONObject(i).getJSONObject("start_location");
                    double temp_lat = Double.parseDouble(jsonOBJ_temp.getString("lat"));
                    double temp_lng = Double.parseDouble(jsonOBJ_temp.getString("lng"));
                    Log.i(TAG,"place : "+temp_lat+","+temp_lng);
                    terminalList.add(new LatLng(temp_lat,temp_lng));
                }
                terminalList.add(end_latlng);
                setRouteOnMapPart02(terminalList);
            }
            catch(Exception e){
                Log.i(TAG,"Error : ");
                e.printStackTrace();
            }
            return null;
        }
    }
}
