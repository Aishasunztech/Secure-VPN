package de.blinkt.openvpn.activities;

import android.app.Fragment;
import android.os.Bundle;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.Utils;
import de.blinkt.openvpn.fragments.GraphFragment;
import de.blinkt.openvpn.fragments.Settings_Allowed_Apps;

public class FragmentShowMainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_show_main);


        if(getIntent()!=null  && getIntent().getStringExtra(MainActivity.SHOW_FRAGMENT_TAG)!=null){

            String fragment  = getIntent().getStringExtra(MainActivity.SHOW_FRAGMENT_TAG);

            if(fragment.equals(MainActivity.SHOW_FRAGMENT_APPS)){
                Settings_Allowed_Apps settings_allowed_apps = new Settings_Allowed_Apps();
                Bundle bundle = new Bundle();

                bundle.putString(getPackageName() + ".profileUUID", Utils.currentVpnProfile.getUUIDString());
                settings_allowed_apps.setArguments(bundle);
                showfragment(settings_allowed_apps);
            }else if (fragment.equals(MainActivity.SHOW_FRAGMENT_GRPH)){
                GraphFragment graphFragment = new GraphFragment();
                showfragment(graphFragment);
            }

        }else{
            finish();
        }



    }

    private void showfragment(Fragment fragment) {

        getFragmentManager().beginTransaction()
                .add(R.id.container_main_fragment, fragment).commit();

    }
}
