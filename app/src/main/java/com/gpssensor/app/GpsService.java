package com.gpssensor.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class GpsService extends Service {
    LocationManager lm; LocationListener listener; String target;
    FirebaseDatabase db;
    public int onStartCommand(Intent i,int flags,int id){
        target=i.getStringExtra("target"); if(target==null)target="target1";
        FirebaseApp.initializeApp(this);
        db=FirebaseDatabase.getInstance("https://meme-project-cab7f-default-rtdb.asia-southeast1.firebasedatabase.app/");
        if(FirebaseAuth.getInstance().getCurrentUser()==null) FirebaseAuth.getInstance().signInAnonymously();
        startForeground(77,notification());
        startLocation(); return START_STICKY;
    }
    Notification notification(){
        NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)n.createNotificationChannel(new NotificationChannel("gps","GPS Realtime",NotificationManager.IMPORTANCE_LOW));
        return new NotificationCompat.Builder(this,"gps").setContentTitle("GPS Sensor Family")
            .setContentText("GPS realtime aktif: "+target).setSmallIcon(android.R.drawable.ic_menu_mylocation).build();
    }
    void startLocation(){
        lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission("android.permission.ACCESS_FINE_LOCATION")!=PackageManager.PERMISSION_GRANTED)return;
        listener=new LocationListener(){
            public void onLocationChanged(Location l){
                Map<String,Object> m=new HashMap<>();
                m.put("latitude",l.getLatitude());m.put("longitude",l.getLongitude());m.put("accuracy",l.getAccuracy());m.put("time",System.currentTimeMillis());
                db.getReference("devices").child(target).child("location").setValue(m);
                db.getReference("devices").child(target).child("locationHistory").push().setValue(m);
            }
        };
        try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,5,listener);}
        catch(Exception e){}
        try{lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000,5,listener);}
        catch(Exception e){}
    }
    public IBinder onBind(Intent i){return null;}
    public void onDestroy(){if(lm!=null&&listener!=null)lm.removeUpdates(listener);super.onDestroy();}
}
