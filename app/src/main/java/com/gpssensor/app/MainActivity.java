package com.gpssensor.app;

import android.Manifest;
import android.app.*;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class MainActivity extends AppCompatActivity {
    static final int REQ = 100;
    FirebaseDatabase db;
    FirebaseAuth auth;
    String role = "ADMIN", target = "target1";
    LinearLayout box;
    TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance(
            "https://meme-project-cab7f-default-rtdb.asia-southeast1.firebasedatabase.app/");
        showRole();
    }

    TextView tv(String s) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(16); t.setPadding(20,16,20,16); return t;
    }
    Button btn(String s, View.OnClickListener l) {
        Button b=new Button(this); b.setText(s); b.setOnClickListener(l); return b;
    }

    void base(String title) {
        ScrollView sv=new ScrollView(this);
        box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(24,24,24,24);
        box.addView(tv(title)); sv.addView(box); setContentView(sv);
    }

    void showRole() {
        base("GPS SENSOR FAMILY\n\nPilih peran");
        box.addView(btn("ADMIN", v->{role="ADMIN"; showAdmin();}));
        box.addView(btn("TARGET 1", v->{role="TARGET"; target="target1"; showTarget();}));
        box.addView(btn("TARGET 2", v->{role="TARGET"; target="target2"; showTarget();}));
        box.addView(btn("TARGET 3", v->{role="TARGET"; target="target3"; showTarget();}));
    }

    void ensureAuth(Runnable r) {
        if(auth.getCurrentUser()!=null){r.run();return;}
        auth.signInAnonymously().addOnSuccessListener(x->r.run())
            .addOnFailureListener(e->Toast.makeText(this,"Firebase login gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }

    void showAdmin() {
        base("ADMIN\n\nMemantau Target 1, 2 dan 3");
        status=tv("Menghubungkan Firebase...");
        box.addView(status);
        box.addView(btn("Segarkan",v->listenTargets()));
        box.addView(btn("Kembali pilih peran",v->showRole()));
        listenTargets();
    }

    void listenTargets() {
        ensureAuth(()->db.getReference("devices").addValueEventListener(new ValueEventListener(){
            public void onDataChange(DataSnapshot s){
                StringBuilder x=new StringBuilder("STATUS TARGET\n\n");
                for(int i=1;i<=3;i++){
                    DataSnapshot d=s.child("target"+i);
                    x.append("TARGET ").append(i).append("\n");
                    x.append("Nama: ").append(d.child("name").getValue(String.class)).append("\n");
                    x.append("GPS: ").append(d.child("location/latitude").getValue()).append(", ")
                     .append(d.child("location/longitude").getValue()).append("\n");
                    x.append("Update: ").append(d.child("location/time").getValue()).append("\n");
                    x.append("Izin: ").append(d.child("permissions").getValue()).append("\n\n");
                }
                status.setText(x.toString());
            }
            public void onCancelled(DatabaseError e){status.setText("Firebase: "+e.getMessage());}
        }));
    }

    void showTarget() {
        base("TARGET: "+target.toUpperCase());
        EditText name=new EditText(this); name.setHint("Nama perangkat"); box.addView(name);
        TextView p=tv(permissionSummary()); box.addView(p);
        box.addView(btn("Minta izin GPS + Sensor",v->{requestPermissions(); p.setText(permissionSummary());}));
        box.addView(btn("Buka Pengaturan Izin Aplikasi",v->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName())))));
        box.addView(btn("Buka Usage Access",v->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        box.addView(btn("Buka Notification Access",v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))));
        box.addView(btn("Tampilkan Sensor Perangkat",v->showSensors()));
        box.addView(btn("Kirim Status ke Firebase",v->saveStatus(name.getText().toString())));
        box.addView(btn("Mulai GPS Realtime",v->startGps(name.getText().toString())));
        box.addView(btn("Hentikan GPS",v->stopService(new Intent(this,GpsService.class))));
        box.addView(btn("Uji Kamera (dengan izin)",v->cameraPermission()));
        box.addView(btn("Uji Mikrofon (dengan izin)",v->micPermission()));
        box.addView(btn("Baca aktivitas aplikasi 1 jam",v->readUsage()));
        box.addView(btn("Kembali pilih peran",v->showRole()));
    }

    String permissionSummary(){
        return "IZIN ANDROID\nGPS: "+ok(Manifest.permission.ACCESS_FINE_LOCATION)+
        "\nKamera: "+ok(Manifest.permission.CAMERA)+
        "\nMikrofon: "+ok(Manifest.permission.RECORD_AUDIO)+
        "\nKontak: "+ok(Manifest.permission.READ_CONTACTS)+
        "\nTelepon: "+ok(Manifest.permission.READ_PHONE_STATE)+
        "\nSensor tubuh: "+ok(Manifest.permission.BODY_SENSORS)+
        "\nNotifikasi: "+(Build.VERSION.SDK_INT<33||ok(Manifest.permission.POST_NOTIFICATIONS));
    }
    String ok(String p){return checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED?"DIIZINKAN":"DITOLAK";}

    void requestPermissions(){
        ArrayList<String> a=new ArrayList<>();
        String[] ps={Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,Manifest.permission.BODY_SENSORS};
        for(String p:ps) if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED) a.add(p);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            a.add(Manifest.permission.POST_NOTIFICATIONS);
        if(!a.isEmpty()) requestPermissions(a.toArray(new String[0]),REQ);
        else Toast.makeText(this,"Izin utama sudah diberikan",Toast.LENGTH_SHORT).show();
    }

    void saveStatus(String name){
        ensureAuth(()->{
            Map<String,Object> m=new HashMap<>();
            m.put("name",name); m.put("role",target); m.put("permissions",permissionSummary());
            m.put("battery",getBattery()); m.put("updated",System.currentTimeMillis());
            db.getReference("devices").child(target).updateChildren(m)
            .addOnSuccessListener(v->Toast.makeText(this,"Status tersimpan",Toast.LENGTH_SHORT).show());
        });
    }

    String getBattery(){
        Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return i==null?"?":(i.getIntExtra("level",-1)+"%");
    }

    void startGps(String name){
        ensureAuth(()->{
            saveStatus(name);
            if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Izinkan GPS dahulu",Toast.LENGTH_LONG).show(); return;
            }
            Intent i=new Intent(this,GpsService.class); i.putExtra("target",target);
            if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
            Toast.makeText(this,"GPS realtime dimulai",Toast.LENGTH_SHORT).show();
        });
    }

    void stopGps(){stopService(new Intent(this,GpsService.class));}
    void cameraPermission(){ if(ok(Manifest.permission.CAMERA).equals("DIIZINKAN")) Toast.makeText(this,"Kamera diizinkan. Aplikasi tidak menyalakan kamera secara diam-diam.",Toast.LENGTH_LONG).show(); else requestPermissions(new String[]{Manifest.permission.CAMERA},REQ); }
    void micPermission(){ if(ok(Manifest.permission.RECORD_AUDIO).equals("DIIZINKAN")) Toast.makeText(this,"Mikrofon diizinkan. Penggunaan tetap harus terlihat oleh pemakai.",Toast.LENGTH_LONG).show(); else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ); }

    void showSensors(){
        base("SENSOR YANG TERSEDIA");
        SensorManager sm=(SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> list=sm.getSensorList(Sensor.TYPE_ALL);
        for(Sensor s:list) box.addView(tv(s.getName()+"\nVendor: "+s.getVendor()+"\nType: "+s.getType()));
        box.addView(btn("Kembali",v->showTarget()));
    }

    void readUsage(){
        if(Build.VERSION.SDK_INT<21){Toast.makeText(this,"Tidak didukung",Toast.LENGTH_SHORT).show();return;}
        UsageStatsManager u=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
        long now=System.currentTimeMillis();
        List<UsageStats> l=u.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,now-3600000,now);
        StringBuilder s=new StringBuilder("AKTIVITAS APLIKASI 1 JAM\n\n");
        for(UsageStats x:l) if(x.getTotalTimeInForeground()>0)
            s.append(x.getPackageName()).append(" — ").append(x.getTotalTimeInForeground()/1000).append(" detik\n");
        new AlertDialog.Builder(this).setMessage(s.length()>30?s.toString():"Belum ada data. Pastikan Usage Access sudah diizinkan.")
            .setPositiveButton("OK",null).show();
    }
}
