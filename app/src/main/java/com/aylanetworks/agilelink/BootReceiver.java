package com.aylanetworks.agilelink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by KLiu on 7/21/16.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent wearService = new Intent(context, WearUpdateService.class);
            context.startService(wearService);
        }
    }
}
