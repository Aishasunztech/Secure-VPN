package de.blinkt.openvpn;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import de.blinkt.openvpn.activities.DisconnectVPN;

public class MyDissconnectService extends Service {

    public static final String ACTION_DISCONNECT = "DISSOCNET_VPN_NOW";


    DisconnectVPN disconnectVPN = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (disconnectVPN == null) {
            disconnectVPN = new DisconnectVPN(this, DisconnectVPN.DISCONNECT_VPN_NOTIFICATION);
        }

        return super.onStartCommand(intent, flags, startId);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(Utils.TAG, "onDestroy: parent service ");
    }
}
