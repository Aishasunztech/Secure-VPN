/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.MyDissconnectService;
import de.blinkt.openvpn.Utils;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Created by arne on 13.10.13.
 */
public class DisconnectVPN implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private IOpenVPNServiceInternal mService;
    private ProgressDialog progressDialog;



    static final String DISCONNECT_VPN_BTN = "DISCONNECT_VPN_BTN";
    public static final String DISCONNECT_VPN_NOTIFICATION = "DISCONNECT_VPN_NOTIFICATION";
    public static final String DISCONNECT_VPN_RESTART = "DISCONNECT_VPN_RESTART";
    public static final String DISCONNECT_VPN_RESTART_APPS_REFRESH = "DISCONNECT_VPN_RESTART_APPS_REFRESH";
    public  String DISCONNECT_VPN_TAG = "DISCONNECT_VPN_TAG";


    private Context context;

    public  DisconnectListener disconnectListener;




    public DisconnectVPN(Context context, String type){
        this.context = context;
        DISCONNECT_VPN_TAG = type;
        try {
            disconnectListener = (DisconnectListener) context;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //  Toast.makeText(context, "disconnect call", Toast.LENGTH_SHORT).show();


        Intent intent = new Intent(context, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        showDisconnectDialog();

    }

    public interface DisconnectListener{
        void onVpnDisconnect(boolean isReconnect);
    }



    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };

    private void showDisconnectDialog() {

        new Handler().postDelayed(() -> {
            onClick(new Dialog(context), DialogInterface.BUTTON_POSITIVE);
            if(progressDialog!=null){progressDialog.dismiss();

            }
        },2000);


    }



    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            ProfileManager.setConntectedVpnProfileDisconnected(context);
            if (mService != null) {
                try {
                    mService.stopVPN(false);

                    if(DISCONNECT_VPN_TAG.equals(DISCONNECT_VPN_RESTART)){

                        if(disconnectListener!=null){
                            disconnectListener.onVpnDisconnect(true);
                        }


                    }else if(DISCONNECT_VPN_TAG.equals(DISCONNECT_VPN_RESTART_APPS_REFRESH)){

                        if(disconnectListener!=null){
                            disconnectListener.onVpnDisconnect(true);
                        }
                    }else if(DISCONNECT_VPN_TAG.equals(DISCONNECT_VPN_BTN)){

                        if(disconnectListener!=null){
                            disconnectListener.onVpnDisconnect(false);
                        }
                    }else if(DISCONNECT_VPN_TAG.equals(DISCONNECT_VPN_NOTIFICATION)){

                        try {
                            ((MainActivity)context).updateView(MainActivity.disconnected_view,context);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }


                } catch (RemoteException e) {
                    VpnStatus.logException(e);
                    Log.i(Utils.TAG, "onClick: Dialog catch block   .... "+ e.getMessage());
                }
            }else{
                Log.i(Utils.TAG, "onClick: Dialog Interface mService!=null");
            }
        } else if (which == DialogInterface.BUTTON_NEUTRAL) {
            Intent intent = new Intent(context, LaunchVPN.class);
            intent.putExtra(LaunchVPN.EXTRA_KEY, VpnStatus.getLastConnectedVPNProfile());
            intent.setAction(Intent.ACTION_MAIN);
            context.startActivity(intent);
        }
        Log.i(Utils.TAG, "onPause: Disconnect Vpn onPause");

        context.unbindService(mConnection);

        context.stopService(new Intent(context, MyDissconnectService.class));

        Collection<VpnProfile> profiles = ProfileManager.getInstance(context).getProfiles();
            Utils.currentVpnProfile = new ArrayList<>(profiles).get(Utils.getSelectedCountry(context));

    }

    @Override
    public void onCancel(DialogInterface dialog) {

    }
}
