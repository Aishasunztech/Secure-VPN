package de.blinkt.openvpn;

import android.app.DialogFragment;

/**
 * @author Muhammad Nadeem
 * @Date 5/22/2019.
 */
public class VpnFragment extends DialogFragment {

    public VpnFragment(){
        setCancelable(true);
    }


    public boolean isFragmentFine(){

        return isAdded() && getActivity()!=null;

    }


}
