package com.etsisi.pas.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.etsisi.pas.auth.models.IpLocationDB;
import com.etsisi.pas.auth.models.Iplocation;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseUser user;
    private FirebaseDatabase database;
    private ArrayList<IpLocationDB> db;
    public static final String EXTRA_MESSAGE = "com.etsisi.pas.auth.ip";
    public static final String HISTORIAL = "com.etsisi.pas.auth.historial";
    private static final int RC_SIGN_IN = 123;
    private static final int QUERY_RQ = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Instanciamos las variables necesarias para el create:
        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance("https://fir-ui-c8a1e-default-rtdb.europe-west1.firebasedatabase.app/");
        EditText ip = (EditText) findViewById(R.id.ip);
        Button query = findViewById(R.id.query);
        FloatingActionButton signout = findViewById(R.id.signoutbut);

        //Solicitamos permiso para el acceso al GPS si no se ha concedido previamente:
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        //Revisamos si el usuario esta ya logeado, si no, iniciamos la acción de log in y recuperamos su historial
        if (user != null) {
            CharSequence text = getResources().getString(R.string.welcome)+" "+ user.getEmail()+"!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
            DatabaseReference myRef = database.getReference("historial_"+queryEmail(user.getEmail()));
            myRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        Log.d("firebase", String.valueOf(task.getResult().getValue()));
                        if(task.getResult().getValue()==null) {
                            db = new ArrayList<IpLocationDB>();
                        }else{
                            RecyclerView listaHistorial = (RecyclerView) findViewById(R.id.listaHistorial);
                            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
                            listaHistorial.setLayoutManager(mLayoutManager);
                            Type listType = new TypeToken<ArrayList<IpLocationDB>>() {}.getType();
                            db = new Gson().fromJson(String.valueOf(task.getResult().getValue()), listType);

                            CustomAdapter adapter = new CustomAdapter(db);
                            listaHistorial.setAdapter(adapter);
                        }

                    }
                }
            });
        } else {
            createSignInIntent();
        }

        //Implementamos la función principal, revisamos que la ip es correcta e iniciamos el nuevo intent
        query.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,
                        QueryActivity.class);

                String ipString = ip.getText().toString();
                if(validateip(ipString)){
                    Gson gson = new Gson();
                    intent.putExtra(EXTRA_MESSAGE, ipString);
                    intent.putExtra(HISTORIAL, gson.toJson(db));
                    startActivityForResult(intent,QUERY_RQ);
                }else {
                    ip.setError(getResources().getString(R.string.ipError));
                }
            }
        });
        //Implementamos la función de sign out, salimos de la aplicación tras el log out
        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //FirebaseAuth.getInstance().signOut();
                signOut();

            }
        });
    }

    private String queryEmail (String email){
        return email.replace('.','_');
    }
    private boolean validateip(String ipString){
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ipString.matches(PATTERN);
    }
    public void createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_create_intent]
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                user = FirebaseAuth.getInstance().getCurrentUser();
                CharSequence text = getResources().getString(R.string.welcome)+" "+ user.getEmail()+"!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(this, text, duration);
                toast.show();
                DatabaseReference myRef = database.getReference("historial_"+queryEmail(user.getEmail()));
                myRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("firebase", "Error getting data", task.getException());
                        } else {
                            Log.d("firebase", String.valueOf(task.getResult().getValue()));
                            if(task.getResult().getValue()==null) {
                                db = new ArrayList<IpLocationDB>();
                            }else{
                                RecyclerView listaHistorial = (RecyclerView) findViewById(R.id.listaHistorial);
                                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
                                listaHistorial.setLayoutManager(mLayoutManager);
                                Type listType = new TypeToken<ArrayList<IpLocationDB>>() {}.getType();
                                db = new Gson().fromJson(String.valueOf(task.getResult().getValue()), listType);

                                CustomAdapter adapter = new CustomAdapter(db);
                                listaHistorial.setAdapter(adapter);
                            }

                        }
                    }
                });
            } else {
                // Como algo ha fallado durante el login, cerramos la aplicación:
                finish();
                System.exit(0);
            }
        }else{
            //Tratamos la respuesta de la activity de query:
            if (requestCode == QUERY_RQ) {
                String result=data.getStringExtra("result");

                RecyclerView listaHistorial = (RecyclerView) findViewById(R.id.listaHistorial);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
                listaHistorial.setLayoutManager(mLayoutManager);
                Type listType = new TypeToken<ArrayList<IpLocationDB>>() {}.getType();
                db = new Gson().fromJson(result, listType);

                CustomAdapter adapter = new CustomAdapter(db);
                listaHistorial.setAdapter(adapter);
            }
        }
    }
    public void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Context context = getApplicationContext();
                        CharSequence text = getResources().getString(R.string.seeyousoon);
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();

                        finish();
                    }
                });
    }
    private static class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private ArrayList<IpLocationDB>  localDataSet;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView iplist;
            private final TextView fecha;

            public ViewHolder(View view) {
                super(view);
                // Define click listener for the ViewHolder's View

                iplist = (TextView) view.findViewById(R.id.iplist);
                fecha = (TextView) view.findViewById(R.id.fechabusqueda);
            }

            public TextView getipView() {
                return iplist;
            }
            public TextView getfechaView() {
                return fecha;
            }
        }

        public CustomAdapter(ArrayList<IpLocationDB> dataSet) {
            localDataSet = dataSet;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            // Create a new view, which defines the UI of the list item
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.ip_row_item, viewGroup, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {

            viewHolder.getipView().setText(localDataSet.get(position).getIplocation().getIp());
            viewHolder.getfechaView().setText(localDataSet.get(position).getFecha());
        }

        @Override
        public int getItemCount() {
            return localDataSet.size();
        }
    }
}