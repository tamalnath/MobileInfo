package org.tamal.mobileinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class GenericReceiver extends BroadcastReceiver {

    private static final String TAG = "GenericReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String str = String.format("Action: %s, Data: %s", intent.getAction(), intent.getDataString());
        Log.i(TAG, str);
        Toast.makeText(context, str, Toast.LENGTH_LONG).show();

    }
}
