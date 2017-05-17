package com.itsmartreach.iTalkieDaemon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.itsmartreach.libzm.Constants;

import static android.R.attr.autoStart;

public class AutoStartReceiver extends BroadcastReceiver {
    public AutoStartReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);


        if ( preferences != null && preferences.getBoolean("auto_start",true) ){
            Log.i(Constants.TAG,"autoStart = true");

            context.startActivity(new Intent(context, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
