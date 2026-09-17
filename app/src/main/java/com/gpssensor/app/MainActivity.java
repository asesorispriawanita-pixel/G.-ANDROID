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

    String role = "ADMIN";
    String target = "target1";

    LinearLayout box;
    TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        FirebaseApp.initializeApp(this);

        auth = FirebaseAuth.getInstance();

        db = FirebaseDatabase.getInstance(
                "https://meme-project-cab7f-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );

        showRole();
    }

    TextView tv(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setPadding(20, 16, 20, 16);
        return t;
    }

    Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setOnClickListener(l);
        return b;
    }

    void base(String title) {
        ScrollView sv = new ScrollView(this);

        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 24, 24, 24);

        box.addView(tv(title));

        sv.addView(box);

        setContentView(sv);
    }

    // =========================================================
    // PILIH PERAN
    // =========================================================

    void showRole() {

        base("GPS SENSOR FAMILY\n\nPilih peran");

        box.addView(
                btn("ADMIN", v -> {
                    role = "ADMIN";
                    showAdmin();
                })
        );

        box.addView(
                btn("TARGET 1", v -> {
                    role = "TARGET";
                    target = "target1";
                    showTarget();
                })
        );

        box.addView(
                btn("TARGET 2", v -> {
                    role = "TARGET";
                    target = "target2";
                    showTarget();
                })
        );

        box.addView(
                btn("TARGET 3", v -> {
                    role = "TARGET";
                    target = "target3";
                    showTarget();
                })
        );
    }

    // =========================================================
    // FIREBASE LOGIN
    // =========================================================

    void ensureAuth(Runnable r) {

        if (auth.getCurrentUser() != null) {
            r.run();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(x -> r.run())
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Firebase login gagal:\n" + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =========================================================
    // ADMIN
    // =========================================================

    void showAdmin() {

        base("ADMIN\n\nMemantau Target 1, 2 dan 3");

        status = tv("Menghubungkan Firebase...");

        box.addView(status);

        box.addView(
                btn("Segarkan", v -> listenTargets())
        );

        box.addView(
                btn("Kembali pilih peran", v -> showRole())
        );

        listenTargets();
    }

    void listenTargets() {

        ensureAuth(() -> {

            db.getReference("devices")
                    .addValueEventListener(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot s) {

                            StringBuilder x =
                                    new StringBuilder("STATUS TARGET\n\n");

                            for (int i = 1; i <= 3; i++) {

                                String tName = "target" + i;

                                DataSnapshot d =
                                        s.child(tName);

                                String nama =
                                        d.child("name")
                                                .getValue(String.class);

                                Object lat =
                                        d.child("location")
                                                .child("latitude")
                                                .getValue();

                                Object lon =
                                        d.child("location")
                                                .child("longitude")
                                                .getValue();

                                Object waktu =
                                        d.child("location")
                                                .child("time")
                                                .getValue();

                                String izin =
                                        d.child("permissions")
                                                .getValue(String.class);

                                x.append("TARGET ")
                                        .append(i)
                                        .append("\n");

                                x.append("Nama: ")
                                        .append(nama == null ? "-" : nama)
                                        .append("\n");

                                x.append("GPS: ")
                                        .append(lat == null ? "-" : lat)
                                        .append(", ")
                                        .append(lon == null ? "-" : lon)
                                        .append("\n");

                                x.append("Update: ")
                                        .append(waktu == null ? "-" : waktu)
                                        .append("\n");

                                x.append("Izin: ")
                                        .append(izin == null ? "-" : izin)
                                        .append("\n");

                                x.append("\n");

                                // Tombol Google Maps jika koordinat tersedia
                                if (lat != null && lon != null) {

                                    final String mapLat =
                                            String.valueOf(lat);

                                    final String mapLon =
                                            String.valueOf(lon);

                                    Button mapButton =
                                            new Button(this);

                                    mapButton.setText(
                                            "Buka TARGET "
                                                    + i
                                                    + " di Google Maps"
                                    );

                                    mapButton.setOnClickListener(v -> {

                                        Uri uri = Uri.parse(
                                                "geo:"
                                                        + mapLat
                                                        + ","
                                                        + mapLon
                                                        + "?q="
                                                        + mapLat
                                                        + ","
                                                        + mapLon
                                        );

                                        Intent intent =
                                                new Intent(
                                                        Intent.ACTION_VIEW,
                                                        uri
                                                );

                                        try {

                                            startActivity(intent);

                                        } catch (Exception e) {

                                            Toast.makeText(
                                                    this,
                                                    "Google Maps tidak tersedia",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    });

                                    box.addView(mapButton);
                                }
                            }

                            status.setText(x.toString());
                        }

                        @Override
                        public void onCancelled(DatabaseError e) {

                            status.setText(
                                    "Firebase error:\n"
                                            + e.getMessage()
                            );
                        }
                    });
        });
    }

    // =========================================================
    // TARGET
    // =========================================================

    void showTarget() {

        base("TARGET: " + target.toUpperCase());

        EditText name = new EditText(this);

        name.setHint("Nama perangkat");

        box.addView(name);

        TextView p = tv(permissionSummary());

        box.addView(p);

        // PERBAIKAN:
        // sebelumnya memanggil requestPermissions() tanpa parameter.
        // sekarang memanggil method kita sendiri.

        box.addView(
                btn("Minta izin GPS + Sensor", v -> {

                    requestAppPermissions();

                    p.setText(permissionSummary());
                })
        );

        box.addView(
                btn(
                        "Buka Pengaturan Izin Aplikasi",
                        v -> startActivity(
                                new Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse(
                                                "package:"
                                                        + getPackageName()
                                        )
                                )
                        )
                )
        );

        box.addView(
                btn(
                        "Buka Usage Access",
                        v -> startActivity(
                                new Intent(
                                        Settings.ACTION_USAGE_ACCESS_SETTINGS
                                )
                        )
                )
        );

        box.addView(
                btn(
                        "Buka Notification Access",
                        v -> startActivity(
                                new Intent(
                                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                                )
                        )
                )
        );

        box.addView(
                btn(
                        "Tampilkan Sensor Perangkat",
                        v -> showSensors()
                )
        );

        box.addView(
                btn(
                        "Kirim Status ke Firebase",
                        v -> saveStatus(
                                name.getText().toString()
                        )
                )
        );

        box.addView(
                btn(
                        "Mulai GPS Realtime",
                        v -> startGps(
                                name.getText().toString()
                        )
                )
        );

        box.addView(
                btn(
                        "Hentikan GPS",
                        v -> stopGps()
                )
        );

        box.addView(
                btn(
                        "Uji Kamera (dengan izin)",
                        v -> cameraPermission()
                )
        );

        box.addView(
                btn(
                        "Uji Mikrofon (dengan izin)",
                        v -> micPermission()
                )
        );

        box.addView(
                btn(
                        "Baca aktivitas aplikasi 1 jam",
                        v -> readUsage()
                )
        );

        box.addView(
                btn(
                        "Kembali pilih peran",
                        v -> showRole()
                )
        );
    }

    // =========================================================
    // RINGKASAN IZIN
    // =========================================================

    String permissionSummary() {

        return "IZIN ANDROID\n"
                + "GPS: "
                + ok(Manifest.permission.ACCESS_FINE_LOCATION)

                + "\nKamera: "
                + ok(Manifest.permission.CAMERA)

                + "\nMikrofon: "
                + ok(Manifest.permission.RECORD_AUDIO)

                + "\nKontak: "
                + ok(Manifest.permission.READ_CONTACTS)

                + "\nTelepon: "
                + ok(Manifest.permission.READ_PHONE_STATE)

                + "\nSensor tubuh: "
                + ok(Manifest.permission.BODY_SENSORS)

                + "\nNotifikasi: "
                + (
                    Build.VERSION.SDK_INT < 33
                    ? "DIIZINKAN"
                    : ok(Manifest.permission.POST_NOTIFICATIONS)
                );
    }

    String ok(String p) {

        if (Build.VERSION.SDK_INT < 23) {
            return "DIIZINKAN";
        }

        return checkSelfPermission(p)
                == PackageManager.PERMISSION_GRANTED
                ? "DIIZINKAN"
                : "DITOLAK";
    }

    // =========================================================
    // REQUEST PERMISSION
    // =========================================================

    void requestAppPermissions() {

        ArrayList<String> permissions =
                new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23) {

            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.ACCESS_FINE_LOCATION
            );

            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            );

            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.CAMERA
            );

            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.RECORD_AUDIO
            );

            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.READ_CONTACTS
            );

            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.READ_PHONE_STATE
            );

            // BODY_SENSORS hanya relevan pada perangkat yang mendukungnya
            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.BODY_SENSORS
            );
        }

        if (Build.VERSION.SDK_INT >= 33) {

            addPermissionIfNeeded(
                    permissions,
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }

        if (!permissions.isEmpty()) {

            // PENTING:
            // Ini adalah Android API requestPermissions(),
            // bukan requestAppPermissions().

            requestPermissions(
                    permissions.toArray(
                            new String[0]
                    ),
                    REQ
            );

        } else {

            Toast.makeText(
                    this,
                    "Izin utama sudah diberikan",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    void addPermissionIfNeeded(
            ArrayList<String> list,
            String permission
    ) {

        if (checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {

            list.add(permission);
        }
    }

    // =========================================================
    // HASIL PERMISSION
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQ) {

            Toast.makeText(
                    this,
                    "Permintaan izin selesai. Periksa status izin di layar.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // SIMPAN STATUS
    // =========================================================

    void saveStatus(String name) {

        ensureAuth(() -> {

            Map<String, Object> m =
                    new HashMap<>();

            m.put(
                    "name",
                    name.trim().isEmpty()
                            ? target
                            : name.trim()
            );

            m.put(
                    "role",
                    target
            );

            m.put(
                    "permissions",
                    permissionSummary()
            );

            m.put(
                    "battery",
                    getBattery()
            );

            m.put(
                    "updated",
                    System.currentTimeMillis()
            );

            db.getReference("devices")
                    .child(target)
                    .updateChildren(m)
                    .addOnSuccessListener(v ->

                            Toast.makeText(
                                    this,
                                    "Status tersimpan",
                                    Toast.LENGTH_SHORT
                            ).show()
                    )
                    .addOnFailureListener(e ->

                            Toast.makeText(
                                    this,
                                    "Gagal menyimpan:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
        });
    }

    // =========================================================
    // BATTERY
    // =========================================================

    String getBattery() {

        Intent i =
                registerReceiver(
                        null,
                        new IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (i == null) {
            return "?";
        }

        return i.getIntExtra(
                "level",
                -1
        ) + "%";
    }

    // =========================================================
    // GPS REALTIME
    // =========================================================

    void startGps(String name) {

        ensureAuth(() -> {

            saveStatus(name);

            if (Build.VERSION.SDK_INT >= 23
                    && checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(
                        this,
                        "Izinkan GPS dahulu",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            Intent i =
                    new Intent(
                            this,
                            GpsService.class
                    );

            i.putExtra(
                    "target",
                    target
            );

            if (Build.VERSION.SDK_INT >= 26) {

                startForegroundService(i);

            } else {

                startService(i);
            }

            Toast.makeText(
                    this,
                    "GPS realtime dimulai untuk "
                            + target,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    void stopGps() {

        stopService(
                new Intent(
                        this,
                        GpsService.class
                )
        );

        Toast.makeText(
                this,
                "GPS realtime dihentikan",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // KAMERA
    // =========================================================

    void cameraPermission() {

        if (ok(Manifest.permission.CAMERA)
                .equals("DIIZINKAN")) {

            Toast.makeText(
                    this,
                    "Kamera diizinkan.\n"
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

    void micPermission() {

        if (ok(Manifest.permission.RECORD_AUDIO)
                .equals("DIIZINKAN")) {

            Toast.makeText(
                    this,
                    "Mikrofon diizinkan.\n"
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

    void showSensors() {

        base("SENSOR YANG TERSEDIA");

        SensorManager sm =
                (SensorManager)
                        getSystemService(
                                SENSOR_SERVICE
                        );

        if (sm == null) {

            box.addView(
                    tv("Sensor Manager tidak tersedia.")
            );

            return;
        }

        List<Sensor> list =
                sm.getSensorList(
                        Sensor.TYPE_ALL
                );

        for (Sensor s : list) {

            box.addView(
                    tv(
                            s.getName()
                                    + "\nVendor: "
                                    + s.getVendor()
                                    + "\nType: "
                                    + s.getType()
                    )
            );
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

    void readUsage() {

        if (Build.VERSION.SDK_INT < 21) {

            Toast.makeText(
                    this,
                    "Tidak didukung",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        UsageStatsManager u =
                (UsageStatsManager)
                        getSystemService(
                                USAGE_STATS_SERVICE
                        );

        if (u == null) {

            Toast.makeText(
                    this,
                    "UsageStats tidak tersedia",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        long now =
                System.currentTimeMillis();

        List<UsageStats> l =
                u.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        now - 3600000,
                        now
                );

        StringBuilder s =
                new StringBuilder(
                        "AKTIVITAS APLIKASI 1 JAM\n\n"
                );

        for (UsageStats x : l) {

            if (x.getTotalTimeInForeground() > 0) {

                s.append(
                        x.getPackageName()
                )
                .append(" — ")
                .append(
                        x.getTotalTimeInForeground()
                                / 1000
                )
                .append(" detik\n");
            }
        }

        new AlertDialog.Builder(this)
                .setMessage(
                        s.length() > 30
                                ? s.toString()
                                : "Belum ada data.\n\n"
                                + "Pastikan Usage Access "
                                + "sudah diizinkan."
                )
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }
}
