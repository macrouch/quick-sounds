package com.sleekcoder.quick_sounds;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearableMessageListenerService extends WearableListenerService {

    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    DataAccess da = new DataAccess();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
//            Intent startIntent = new Intent(this, QuickSoundsActivity.class);
//            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(startIntent);
            String filepath = da.getSound(0, "Path", this);
            boolean loop = (da.getSound(0, "Loop", this).equals("true"));

            // Play the first button's sound
            SoundPlayer.playSound(filepath, loop);
        }
    }
}
