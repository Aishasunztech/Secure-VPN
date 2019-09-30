package de.blinkt.openvpn.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.Utils;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.fragments.VPNProfileList;

public class SubscriptionActivity extends BaseActivity implements View.OnClickListener{


    public static ProgressDialog progressDialog;
    public static final String UPDATE_SUBSCRIPTION = "UPDATE_SUBSCRIPTION";
    private static final String TAG = "SubscrActivity_Check";
    private Button btn_3,btn_6,btn_12;
    private static int counterLoad = 0 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = getWindow().getDecorView();
        int visiblility = WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        view.setSystemUiVisibility(visiblility);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_subscription);



        btn_3 = findViewById(R.id.btn_3);
        btn_6 = findViewById(R.id.btn_6);
        btn_12 = findViewById(R.id.btn_12);


        if(getIntent()!=null&& getIntent().getBooleanExtra(UPDATE_SUBSCRIPTION,false)){

            String subscribe_duration = Utils.getSubscribeDuration(SubscriptionActivity.this);

            if(subscribe_duration.equals(Utils.SUBSCRIBE_THREE_MONTHS_TAG)){

                btn_3.setText("Subscribed");

            }else if(subscribe_duration.equals(Utils.SUBSCRIBE_SIX_MONTHS_TAG)){
                btn_6.setText("Subscribed");

            }else if(subscribe_duration.equals(Utils.SUBSCRIBE_twelve_Months_TAG)){
                btn_12.setText("Subscribed");
            }



        }else{

            if(Utils.getSubscribeDuration(SubscriptionActivity.this)!=null){
                loadVpnProfileAndStartActivity();
            }

        }



        btn_3.setOnClickListener(this);
        btn_6.setOnClickListener(this);
        btn_12.setOnClickListener(this);


        progressDialog = new ProgressDialog(SubscriptionActivity.this);
        progressDialog.setMessage("Processing .... ");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

    }


    private void loadVpnProfileAndStartActivity() {
        ArrayList<VpnProfile> setList = new ArrayList<>();

        boolean sortByLRU = Preferences.getDefaultSharedPreferences(SubscriptionActivity.this).getBoolean(VPNProfileList.PREF_SORT_BY_LRU, false);
        Collection<VpnProfile> allvpn = getPM().getProfiles();

        setList.addAll(allvpn);
        if(allvpn!=null && allvpn.size()>0){

            int index = Utils.getSelectedCountry(SubscriptionActivity.this);

            if(index >= allvpn.size()){
                Utils.currentVpnProfile = null;
                startNextActivity(false);
            }else{
                Utils.currentVpnProfile = setList.get(index);
                startNextActivity(true);
            }


        }else{

            Toast.makeText(this, "no data found, please subscribe to access specific location", Toast.LENGTH_SHORT).show();
           // Utils.setSubscribeDuration(SubscriptionActivity.this,false,null);

        }

    }


    private void startImportingFile() {


        if(btn_3.getTag()!=null && btn_6.getTag() == null && btn_12.getTag() == null){

            Utils.ASSETS_FILE = Utils.country_three_months_data.getCountries_profiles()[counterLoad];

        }else if(btn_6.getTag()!=null && btn_3.getTag() == null && btn_12.getTag() == null){

            Utils.ASSETS_FILE = Utils.country_six_months_data.getCountries_profiles()[counterLoad];

        }else if(btn_12.getTag()!=null && btn_3.getTag() == null && btn_6.getTag() == null){

            Utils.ASSETS_FILE = Utils.country_twelve_months_data.getCountries_profiles()[counterLoad];

        }

        if(progressDialog!=null){
            progressDialog.show();
        }


        Log.i(TAG, "startImportingFile: utile file to b load is  "+ Utils.ASSETS_FILE);


        Intent startImport = new Intent(SubscriptionActivity.this, ConfigConverter.class);
        startImport.setAction(ConfigConverter.IMPORT_PROFILE);
        startImport.setData(null);
        startImport.putExtra(Utils.FILE_TAG, Utils.FILE_TYPE_ASSETS);
        startActivityForResult(startImport, VPNProfileList.IMPORT_PROFILE);


    }


    private ProfileManager getPM() {
        return ProfileManager.getInstance(SubscriptionActivity.this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(progressDialog!=null){
            if(progressDialog.isShowing()){ progressDialog.dismiss();}
        }

        if (requestCode == VPNProfileList.IMPORT_PROFILE) {

            // send profile to main activity other view hide completely

            if(btn_3.getTag()!=null && btn_6.getTag() == null && btn_12.getTag() == null){

                if(counterLoad == Utils.country_three_months_data.getCountries_profiles().length - 1){

                    String profileUUID = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);
                    VpnProfile vpnProfile = ProfileManager.get(SubscriptionActivity.this, profileUUID);
                 //   Utils.setSubscribeDuration(SubscriptionActivity.this,true,Utils.SUBSCRIBE_THREE_MONTHS_TAG);
                    Utils.currentVpnProfile = vpnProfile;

                    //  Utils.setSelectedCountry(SubscriptionActivity.this,0);

                    Utils.currentVpnProfile = null;
                    startNextActivity(false);

                }else{

                    counterLoad = counterLoad + 1 ;
                    startImportingFile();

                }

            }else if(btn_6.getTag()!=null && btn_3.getTag() == null && btn_12.getTag() == null){

                Toast.makeText(this, "not defined yet", Toast.LENGTH_SHORT).show();

            }else if(btn_12.getTag()!=null && btn_3.getTag() == null && btn_6.getTag() == null){

                Toast.makeText(this, "not defined yet", Toast.LENGTH_SHORT).show();

            }


        }

    }

    private void startNextActivity(boolean value){


        Log.i(Utils.TAG, "startNextActivity: from Subscription_Activity is ...  "+value);

        Intent intent = new Intent(SubscriptionActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.LAYOUT_TAG,value);
        startActivity(intent);
        finish();




    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){


            case R.id.btn_3:

                if(btn_3.getText().equals("Subscribed")){
                    Toast.makeText(this, "You Already Subscribed this offer", Toast.LENGTH_SHORT).show();
                }else{
                    btn_3.setTag("clicked");
                    btn_6.setTag(null);
                    btn_12.setTag(null);
                    counterLoad = 0 ;
                    startImportingFile();

                }
                break;
            case R.id.btn_6:


                if(btn_6.getText().equals("Subscribed")){

                    Toast.makeText(this, "You Already Subscribed this offer", Toast.LENGTH_SHORT).show();
                }else{
                    btn_3.setTag(null);
                    btn_6.setTag("clicked");
                    btn_12.setTag(null);

                    Toast.makeText(this, "We are working, Available Soon ", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_12:




                if(btn_12.getText().equals("Subscribed")){
                    Toast.makeText(this, "You Already Subscribed this offer", Toast.LENGTH_SHORT).show();

                }else{

                    btn_3.setTag(null);
                    btn_6.setTag(null);
                    btn_12.setTag("clicked");

                    Toast.makeText(this, "We are working, Available Soon ", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;

        }

    }
}
