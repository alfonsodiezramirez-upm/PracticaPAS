package com.etsisi.pas.auth;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.etsisi.pas.auth.models.IpLocationDB;
import com.etsisi.pas.auth.models.Iplocation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class QueryActivity extends AppCompatActivity implements Callback<Iplocation>, LocationListener {
    private String ip;
    protected LocationManager locationManager;
    boolean isGPSEnabled = false;
    private ArrayList<IpLocationDB> historial;

    boolean isNetworkEnabled = false;
    private Gson gson;
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude
    private FloatingActionButton openmap;
    private FloatingActionButton virustotal;
    private FloatingActionButton shodan;

    private FirebaseUser user;
    private FirebaseDatabase database;
    private TextView resultado;
    private TextView ciudad;
    private TextView lat;
    private TextView lon;
    private TextView ipconsultada;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query);
        //Inicializamos todas las variables comunes:
        openmap = findViewById(R.id.openMap);
        openmap.setVisibility(INVISIBLE);
        virustotal = findViewById(R.id.virustotal);
        virustotal.setVisibility(INVISIBLE);
        shodan = findViewById(R.id.shodan);
        shodan.setVisibility(INVISIBLE);
        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance("https://fir-ui-c8a1e-default-rtdb.europe-west1.firebasedatabase.app/");
        resultado = findViewById(R.id.resultado);
        ciudad = findViewById(R.id.ciudad);
        lat = findViewById(R.id.latitude);
        lon = findViewById(R.id.longitude);
        ipconsultada = findViewById(R.id.ipConsultada);
        gson = new Gson();
        getLocation();
        Intent intent = getIntent();

        ip = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        Type listType = new TypeToken<ArrayList<IpLocationDB>>() {}.getType();

        historial = new Gson().fromJson(intent.getStringExtra(MainActivity.HISTORIAL), listType);

        //llamamos con retrofit a la petición para consultar la localización de una ip
        Call<Iplocation> iplocationCall = IpApiAdapter.getApiService().getLocation(ip,"ad013f51134ad296093e0de44ada0353");
        iplocationCall.enqueue(this);

    }

    @Override
    public void onResponse(Call<Iplocation> call, Response<Iplocation> response) {
        if(response.isSuccessful()){

            Iplocation locationip = response.body();

            if(locationip.getLatitude()!=null && locationip.getLongitude()!=null && location !=null) {

                String distancia = new DecimalFormat("#.###").format(distance(locationip.getLatitude(), latitude, locationip.getLongitude(), longitude));
                resultado.setText(distancia+" Km");
                ciudad.setText(locationip.getCity()+"\\"+locationip.getCountryName());
                lat.setText(""+locationip.getLatitude());
                lon.setText(""+locationip.getLongitude());
                ipconsultada.setText(response.body().getIp());
                openmap.setVisibility(VISIBLE);
                openmap.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //abrimos google maps
                        String uri = String.format(Locale.ENGLISH, "geo:%f,%f", locationip.getLatitude(), locationip.getLongitude());
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        startActivity(intent);
                    }
                });
                virustotal.setVisibility(VISIBLE);
                virustotal.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //abrimos virustotal
                        Uri uri = Uri.parse("https://www.virustotal.com/gui/ip-address/"+locationip.getIp()+"/detection");
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                });
                shodan.setVisibility(VISIBLE);
                shodan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //abrimos shodan
                        Uri uri = Uri.parse("https://www.shodan.io/host/"+locationip.getIp());
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                });

                //Almacenamos el resultado en el historial, dado que hacemos un set completo, hacemos append al Arraylist que nos traemos de la actividad principal, ..
                //..tambien le retornamos el valor a la actividad principal, para ahorrarnos una llamada a la base de datos de algo que está ya en memoría
                DatabaseReference myRef = database.getReference("historial_"+queryEmail(user.getEmail()));
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();

                IpLocationDB entrada =  new IpLocationDB(user.getEmail(),dtf.format(now),locationip);
                historial.add(entrada);
                myRef.setValue(gson.toJson(historial));
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result",gson.toJson(historial));
                setResult(Activity.RESULT_OK,returnIntent);
            }else{
                //Si falla el resultado informamos al usuario en la misma actividad:
                ipconsultada.setText(ip);
                resultado.setText("...");
                ciudad.setText(getResources().getString(R.string.noResult));
                ciudad.setTextSize(16);
                lat.setText("...");
                lon.setText("...");
            }

        }
    }
    private String queryEmail(String email){
        return email.replace('.','_');
    }
    @Override
    public void onFailure(Call<Iplocation> call, Throwable t) {
        //Si falla el resultado informamos al usuario en la misma actividad:
        ipconsultada.setText(ip);
        resultado.setText("...");
        ciudad.setText(getResources().getString(R.string.noResult));
        ciudad.setTextSize(16);
        lat.setText("...");
        lon.setText("...");
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        //No hacemos nada, no nos es relevante para el caso de uso
    }
    //Obtenemos la localización via GPS o red Móvil, priorizamos red GPS para cumplir mejor con el enunciado de la práctica aunque la red móvil consume menos batería para el posicionamiento.
    public Location getLocation() {
        try {
            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isGPSEnabled || isNetworkEnabled){
                this.canGetLocation = true;

                if (isGPSEnabled) {

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    Log.d("GPS Enabled", "GPS Enabled");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }

                }
                if (isNetworkEnabled) {
                    if (location == null) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                        }
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        Log.d("Network", "Network");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

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

        return location;
    }
    private double distance(double lat1, double lat2, double lon1, double lon2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c ; // in kilometers

        return distance;
    }
}