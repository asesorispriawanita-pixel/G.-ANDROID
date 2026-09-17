package com.gpssensor.app;

import android.Manifest;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQ = 100;

    private static final String DB_URL =
            "https://meme-project-cab7f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private FirebaseDatabase db;
    private FirebaseAuth auth;

    private String role = "ADMIN";
    private String target = "target1";

    private LinearLayout box;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception ignored) {
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance(DB_URL);

        showRole();
    }

    // =========================================================
    // KOMPONEN UI
    // =========================================================

    private TextView tv(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setPadding(20, 16, 20, 16);
        return t;
    }

    private Button btn(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(listener);
        return b;
    }

    private void base(String title) {
        ScrollView scroll = new ScrollView(this);

        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 24, 24, 24);

        box.addView(tv(title));

        scroll.addView(box);
        setContentView(scroll);
    }

    // =========================================================
    // PILIH PERAN
    // =========================================================

    private void showRole() {

        base("GPS SENSOR FAMILY\n\nPilih peran");

        box.addView(btn("ADMIN", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                role = "ADMIN";
                showAdmin();
            }
        }));

        box.addView(btn("TARGET 1", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                role = "TARGET";
                target = "target1";
                showTarget();
            }
        }));

        box.addView(btn("TARGET 2", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                role = "TARGET";
                target = "target2";
                showTarget();
            }
        }));

        box.addView(btn("TARGET 3", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                role = "TARGET";
                target = "target3";
                showTarget();
            }
        }));
    }

    // =========================================================
    // FIREBASE AUTH
    // =========================================================

    private void ensureAuth(final Runnable action) {

        if (auth.getCurrentUser() != null) {
            action.run();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> action.run())
                .addOnFailureListener(error ->
                        Toast.makeText(
                                MainActivity.this,
                                "Firebase login gagal:\n"
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =========================================================
    // ADMIN
    // =========================================================

    private void showAdmin() {

        base("ADMIN\n\nMemantau Target 1, 2 dan 3");

        status = tv("Menghubungkan Firebase...");
        box.addView(status);

        box.addView(btn("Segarkan", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listenTargets();
            }
        }));

        box.addView(btn("Kembali pilih peran", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRole();
            }
        }));

        listenTargets();
    }

    private void listenTargets() {

        ensureAuth(new Runnable() {
            @Override
            public void run() {

                DatabaseReference reference =
                        db.getReference("devices");

                reference.addValueEventListener(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    DataSnapshot snapshot) {

                                StringBuilder text =
                                        new StringBuilder();

                                text.append(
                                        "STATUS TARGET\n\n"
                                );

                                for (int i = 1; i <= 3; i++) {

                                    String device =
                                            "target" + i;

                                    DataSnapshot data =
                                            snapshot.child(device);

                                    String name =
                                            data.child("name")
                                                    .getValue(
                                                            String.class
                                                    );

                                    Object latitude =
                                            data.child("location")
                                                    .child("latitude")
                                                    .getValue();

                                    Object longitude =
                                            data.child("location")
                                                    .child("longitude")
                                                    .getValue();

                                    Object time =
                                            data.child("location")
                                                    .child("time")
                                                    .getValue();

                                    String permissions =
                                            data.child("permissions")
                                                    .getValue(
                                                            String.class
                                                    );

                                    text.append(
                                            "TARGET "
                                    ).append(i).append("\n");

                                    text.append(
                                            "Nama: "
                                    ).append(
                                            name == null
                                                    ? "-"
                                                    : name
                                    ).append("\n");

                                    text.append(
                                            "GPS: "
                                    ).append(
                                            latitude == null
                                                    ? "-"
                                                    : latitude
                                    ).append(", ").append(
                                            longitude == null
                                                    ? "-"
                                                    : longitude
                                    ).append("\n");

                                    text.append(
                                            "Update: "
                                    ).append(
                                            time == null
                                                    ? "-"
                                                    : time
                                    ).append("\n");

                                    text.append(
                                            "Izin: "
                                    ).append(
                                            permissions == null
                                                    ? "-"
                                                    : permissions
                                    ).append("\n\n");

                                    if (latitude != null
                                            && longitude != null) {

                                        addMapButton(
                                                i,
                                                latitude.toString(),
                                                longitude.toString()
                                        );
                                    }
                                }

                                status.setText(
                                        text.toString()
                                );
                            }

                            @Override
                            public void onCancelled(
                                    DatabaseError error) {

                                status.setText(
                                        "Firebase error:\n"
                                                + error.getMessage()
                                );
                            }
                        }
                );
            }
        });
    }

    // =========================================================
    // GOOGLE MAPS
    // =========================================================

    private void addMapButton(
            final int number,
            final String latitude,
            final String longitude) {

        Button mapButton =
                new Button(MainActivity.this);

        mapButton.setText(
                "Buka TARGET "
                        + number
                        + " di Google Maps"
        );

        mapButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        Uri uri = Uri.parse(
                                "geo:"
                                        + latitude
                                        + ","
                                        + longitude
                                        + "?q="
                                        + latitude
                                        + ","
                                        + longitude
                        );

                        Intent intent =
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        uri
                                );

                        try {

                            MainActivity.this
                                    .startActivity(intent);

                        } catch (Exception error) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Aplikasi peta tidak tersedia",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );

        box.addView(mapButton);
    }

    // =========================================================
    // TARGET
    // =========================================================

    private void showTarget() {

        base(
                "TARGET: "
                        + target.toUpperCase()
        );

        final EditText name =
                new EditText(this);

        name.setHint("Nama perangkat");

        box.addView(name);

        final TextView permissionText =
                tv(permissionSummary());

        box.addView(permissionText);

        box.addView(
                btn(
                        "Minta izin GPS + Sensor",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                requestAppPermissions();

                                permissionText.setText(
                                        permissionSummary()
                                );
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Buka Pengaturan Izin Aplikasi",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                Intent intent =
                                        new Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        );

                                intent.setData(
                                        Uri.parse(
                                                "package:"
                                                        + getPackageName()
                                        )
                                );

                                startActivity(intent);
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Buka Usage Access",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                startActivity(
                                        new Intent(
                                                Settings.ACTION_USAGE_ACCESS_SETTINGS
                                        )
                                );
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Buka Notification Access",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                try {

                                    startActivity(
                                            new Intent(
                                                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                                            )
                                    );

                                } catch (Exception error) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Pengaturan Notification Access tidak tersedia",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Tampilkan Sensor Perangkat",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                showSensors();
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Kirim Status ke Firebase",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                saveStatus(
                                        name.getText()
                                                .toString()
                                );
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Mulai GPS Realtime",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                startGps(
                                        name.getText()
                                                .toString()
                                );
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Hentikan GPS",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                stopGps();
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Uji Kamera (dengan izin)",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                cameraPermission();
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Uji Mikrofon (dengan izin)",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                micPermission();
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Baca aktivitas aplikasi 1 jam",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                readUsage();
                            }
                        }
                )
        );

        box.addView(
                btn(
                        "Kembali pilih peran",
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                showRole();
                            }
                        }
                )
        );
    }

    // =========================================================
    // PERMISSION SUMMARY
    // =========================================================

    private String permissionSummary() {

        String notification;

        if (Build.VERSION.SDK_INT < 33) {
            notification = "DIIZINKAN";
        } else {
            notification =
                    permissionState(
                            Manifest.permission.POST_NOTIFICATIONS
                    );
        }

        return "IZIN ANDROID\n"
                + "GPS: "
                + permissionState(
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                + "\nKamera: "
                + permissionState(
                        Manifest.permission.CAMERA
                )
                + "\nMikrofon: "
                + permissionState(
                        Manifest.permission.RECORD_AUDIO
                )
                + "\nKontak: "
                + permissionState(
                        Manifest.permission.READ_CONTACTS
                )
                + "\nTelepon: "
                + permissionState(
                        Manifest.permission.READ_PHONE_STATE
                )
                + "\nSensor tubuh: "
                + permissionState(
                        Manifest.permission.BODY_SENSORS
                )
                + "\nNotifikasi: "
                + notification;
    }

    private String permissionState(String permission) {

        if (Build.VERSION.SDK_INT < 23) {
            return "DIIZINKAN";
        }

        return checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED
                ? "DIIZINKAN"
                : "DITOLAK";
    }

    // =========================================================
    // REQUEST PERMISSIONS
    // =========================================================

    private void requestAppPermissions() {

        ArrayList<String> permissions =
                new ArrayList<>();

        addPermission(
                permissions,
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        addPermission(
                permissions,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );

        addPermission(
                permissions,
                Manifest.permission.CAMERA
        );

        addPermission(
                permissions,
                Manifest.permission.RECORD_AUDIO
        );

        addPermission(
                permissions,
                Manifest.permission.READ_CONTACTS
        );

        addPermission(
                permissions,
                Manifest.permission.READ_PHONE_STATE
        );

        addPermission(
                permissions,
                Manifest.permission.BODY_SENSORS
        );

        if (Build.VERSION.SDK_INT >= 33) {

            addPermission(
                    permissions,
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }

        if (permissions.isEmpty()) {

            Toast.makeText(
                    MainActivity.this,
                    "Izin utama sudah diberikan",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            // Ini adalah Android requestPermissions().
            // Bukan pemanggilan ulang requestAppPermissions().

            requestPermissions(
                    permissions.toArray(
                            new String[0]
                    ),
                    REQ
            );
        }
    }

    private void addPermission(
            ArrayList<String> list,
            String permission) {

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {

            list.add(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQ) {

            Toast.makeText(
                    MainActivity.this,
                    "Permintaan izin selesai",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // SIMPAN STATUS
    // =========================================================

    private void saveStatus(String deviceName) {

        ensureAuth(new Runnable() {

            @Override
            public void run() {

                Map<String, Object> data =
                        new HashMap<>();

                String finalName =
                        deviceName.trim();

                if (finalName.isEmpty()) {
                    finalName = target;
                }

                data.put(
                        "name",
                        finalName
                );

                data.put(
                        "role",
                        target
                );

                data.put(
                        "permissions",
                        permissionSummary()
                );

                data.put(
                        "battery",
                        getBattery()
                );

                data.put(
                        "updated",
                        System.currentTimeMillis()
                );

                db.getReference("devices")
                        .child(target)
                        .updateChildren(data)
                        .addOnSuccessListener(
                                result ->
                                        Toast.makeText(
                                                MainActivity.this,
                                                "Status tersimpan",
                                                Toast.LENGTH_SHORT
                                        ).show()
                        )
                        .addOnFailureListener(
                                error ->
                                        Toast.makeText(
                                                MainActivity.this,
                                                "Gagal menyimpan:\n"
                                                        + error.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show()
                        );
            }
        });
    }

    // =========================================================
    // BATTERY
    // =========================================================

    private String getBattery() {

        Intent batteryIntent =
                registerReceiver(
                        null,
                        new IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (batteryIntent == null) {
            return "?";
        }

        int level =
                batteryIntent.getIntExtra(
                        "level",
                        -1
                );

        return level + "%";
    }

    // =========================================================
    // START GPS
    // =========================================================

    private void startGps(String deviceName) {

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                    MainActivity.this,
                    "Izinkan GPS dahulu",
                    Toast.LENGTH_LONG
            ).show();

            requestAppPermissions();
            return;
        }

        saveStatus(deviceName);

        Intent intent =
                new Intent(
                        MainActivity.this,
                        GpsService.class
                );

        intent.putExtra(
                "target",
                target
        );

        try {

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            Toast.makeText(
                    MainActivity.this,
                    "GPS realtime dimulai untuk "
                            + target,
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception error) {

            Toast.makeText(
                    MainActivity.this,
                    "Gagal memulai GPS:\n"
                            + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopGps() {

        stopService(
                new Intent(
                        MainActivity.this,
                        GpsService.class
                )
        );

        Toast.makeText(
                MainActivity.this,
                "GPS realtime dihentikan",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // KAMERA
    // =========================================================

    private void cameraPermission() {

        if (permissionState(
                Manifest.permission.CAMERA
        ).equals("DIIZINKAN")) {

            Toast.makeText(
                    MainActivity.this,
                    "Kamera sudah diizinkan.\n"
                            + "Aplikasi tidak menyalakan kamera "
                            + "secara diam-diam.",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA
                    },
                    REQ
            );
        }
    }

    // =========================================================
    // MICROPHONE
    // =========================================================

    private void micPermission() {

        if (permissionState(
                Manifest.permission.RECORD_AUDIO
        ).equals("DIIZINKAN")) {

            Toast.makeText(
                    MainActivity.this,
                    "Mikrofon sudah diizinkan.\n"
                            + "Penggunaan tetap harus terlihat "
                            + "oleh pemakai.",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQ
            );
        }
    }

    // =========================================================
    // SENSOR
    // =========================================================

    private void showSensors() {

        base("SENSOR YANG TERSEDIA");

        SensorManager sensorManager =
                (SensorManager)
                        getSystemService(
                                SENSOR_SERVICE
                        );

        if (sensorManager == null) {

            box.addView(
                    tv("Sensor perangkat tidak tersedia.")
            );

            box.addView(
                    btn(
                            "Kembali",
                            v -> showTarget()
                    )
            );

            return;
        }

        List<Sensor> sensors =
                sensorManager.getSensorList(
                        Sensor.TYPE_ALL
                );

        if (sensors.isEmpty()) {

            box.addView(
                    tv("Tidak ada sensor yang terdeteksi.")
            );

        } else {

            for (Sensor sensor : sensors) {

                String info =
                        sensor.getName()
                                + "\nVendor: "
                                + sensor.getVendor()
                                + "\nType: "
                                + sensor.getType();

                box.addView(
                        tv(info)
                );
            }
        }

        box.addView(
                btn(
                        "Kembali",
                        v -> showTarget()
                )
        );
    }

    // =========================================================
    // USAGE ACCESS
    // =========================================================

    private void readUsage() {

        if (Build.VERSION.SDK_INT < 21) {

            Toast.makeText(
                    MainActivity.this,
                    "Tidak didukung",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        UsageStatsManager manager =
                (UsageStatsManager)
                        getSystemService(
                                USAGE_STATS_SERVICE
                        );

        if (manager == null) {

            Toast.makeText(
                    MainActivity.this,
                    "Usage Access tidak tersedia",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        long now =
                System.currentTimeMillis();

        List<UsageStats> list =
                manager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        now - 3600000,
                        now
                );

        StringBuilder text =
                new StringBuilder();

        text.append(
                "AKTIVITAS APLIKASI 1 JAM\n\n"
        );

        for (UsageStats usage : list) {

            if (usage.getTotalTimeInForeground()
                    > 0) {

                text.append(
                        usage.getPackageName()
                );

                text.append(" — ");

                text.append(
                        usage.getTotalTimeInForeground()
                                / 1000
                );

                text.append(
                        " detik\n"
                );
            }
        }

        String message;

        if (text.length() > 30) {
            message = text.toString();
        } else {
            message =
                    "Belum ada data.\n\n"
                            + "Pastikan Usage Access "
                            + "sudah diizinkan.";
        }

        new AlertDialog.Builder(
                MainActivity.this
        )
                .setMessage(message)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }
}
