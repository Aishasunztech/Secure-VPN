
package de.blinkt.openvpn.activities;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4n.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Collection;

import de.blinkt.openvpn.ConfigWorker;
import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.Utils;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.fragments.AboutFragment;
import de.blinkt.openvpn.fragments.FaqFragment;
import de.blinkt.openvpn.fragments.GeneralSettings;
import de.blinkt.openvpn.fragments.GraphFragment;
import de.blinkt.openvpn.fragments.LogFragment;
import de.blinkt.openvpn.fragments.SendDumpFragment;
import de.blinkt.openvpn.fragments.Settings_Allowed_Apps;
import de.blinkt.openvpn.fragments.VPNProfileList;
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter;
import de.blinkt.openvpn.views.SlidingTabLayout;
import de.blinkt.openvpn.views.TabBarView;

import static de.blinkt.openvpn.Utils.TOUR_STATUS;


public class MainActivity extends AppCompatActivity
        implements View.OnClickListener,
        VpnStatus.StateListener,
        DisconnectVPN.DisconnectListener
        , Settings_Allowed_Apps.AppsListRefreshListener,
        LaunchVPN.LaunchVpnListener {


    public static final String ACTION_DIRECT_DISCONNECT = "ACTION_DIRECT_DISCONNECT";
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private SlidingTabLayout mSlidingTabLayout;
    private TabBarView mTabs;

    private ProgressDialog progressDialog;

    public static final String TAG = "MainActivityCh";

    private ArrayList<VpnProfile> setList = new ArrayList<>();
    private LinearLayout linearLayoutHorizantal;
    private static final String connected_view = "connected_view";
    public static final String disconnected_view = "disconnected_view";
    private static final String TABS_VIEW = "TABS_VIEW";
    private static final String CONSTRAINT_VIEW = "CONSTRAINT_VIEW";
    private RelativeLayout constraintLayout;
    public TextView tv_device_status, tv_2, tv_1;
    private static final int LanuchActivityRequestCode = 899;

    public static final String SHOW_FRAGMENT_TAG = "SHOW_FRAGMENT_TAG";
    public static final String SHOW_FRAGMENT_APPS = "SHOW_FRAGMENT_APPS";
    public static final String SHOW_FRAGMENT_GRPH = "SHOW_FRAGMENT_GRAPH";


    private ImageView img_current_location;
    private TextView txt_current_location;


    private static final String FEATURE_TELEVISION = "android.hardware.type.television";
    private static final String FEATURE_LEANBACK = "android.software.leanback";


    public static final String LAYOUT_TAG = "LAYOUT_TAG";

    private LinearLayout layout_country_bold;

    private static final String connecting = "Connecting..";
    private static final String other = "other..";


    int counterLoad = 0;


    private ImageView statusDot, center_img;
    private TextView connection_text_btn, tvStatus;

    private boolean inter1_load_check = false;
    private boolean inter2_load_check = false;


    private InterstitialAd interstitialAd_1, interstitialAd_2;
    private AdView adView_banner1,  ad;


    protected void onCreate(android.os.Bundle savedInstanceState) {
        setTheme(R.style.AppCompatTHeme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        SharedPreferences sharedPreferences = getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(TOUR_STATUS, false)) {
            Intent intent = new Intent(this, GetStartedActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        MobileAds.initialize(this, getResources().getString(R.string.app_id));

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        inItViews();

        if (Utils.getSubscribeDuration(MainActivity.this) != null) {
            vpnConfig();
        } else {
            loadProfilesFirst();
        }

        adView_banner1 = findViewById(R.id.adView_banner1);
        AdRequest adRequest = new AdRequest.Builder().build();
        ad = new AdView(this);
        ad.setAdSize(AdSize.MEDIUM_RECTANGLE);
        ad.setAdUnitId(getResources().getString(R.string.banner1_id));
        ad.loadAd(adRequest);
        adView_banner1.loadAd(adRequest);
        if (Utils.currentVpnProfile != null) {
            if (isVpnConnected(Utils.currentVpnProfile)) {
                updateView(connected_view, MainActivity.this);

                if (Utils.getCurrentStatus(MainActivity.this).equals(connecting)) {
                    if (tvStatus != null) {

                        tvStatus.setText(connecting);
                        // tvStatus.setGravity(Gravity.CENTER);

                        Utils.setCurrentStatus(MainActivity.this, connecting);
                    }
                }

            } else {
                updateView(disconnected_view, MainActivity.this);
            }

        } else {
            updateView(disconnected_view, MainActivity.this);
            Toast.makeText(this, "Location Not Selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilesFirst() {

        Utils.CURRENT_MONTHS = Utils.SUBSCRIBE_THREE_MONTHS_TAG;

        counterLoad = 0;
        startImportingFile();
    }

    private void vpnConfig() {

        progressDialog.setMessage("Disconnecting ...");
        pagerConfig();
        Settings_Allowed_Apps.appsListRefreshListener = (Settings_Allowed_Apps.AppsListRefreshListener) MainActivity.this;
        LaunchVPN.launchVpnListener = (LaunchVPN.LaunchVpnListener) MainActivity.this;

    }

    private void startImportingFile() {


        if (Utils.CURRENT_MONTHS.equals(Utils.SUBSCRIBE_THREE_MONTHS_TAG)) {

            Utils.ASSETS_FILE = Utils.country_three_months_data.getCountries_profiles()[counterLoad];

        } else if (Utils.CURRENT_MONTHS.equals(Utils.SUBSCRIBE_SIX_MONTHS_TAG)) {

            Utils.ASSETS_FILE = Utils.country_six_months_data.getCountries_profiles()[counterLoad];

        } else if (Utils.CURRENT_MONTHS.equals(Utils.SUBSCRIBE_twelve_Months_TAG)) {

            Utils.ASSETS_FILE = Utils.country_twelve_months_data.getCountries_profiles()[counterLoad];

        }

        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.setMessage("Loading Profile ....");
            progressDialog.show();
        }


        OneTimeWorkRequest insertionWork =
                new OneTimeWorkRequest.Builder(ConfigWorker.class)
                        .build();
        WorkManager.getInstance(this).enqueue(insertionWork);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(insertionWork.getId()).observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                Intent result = new Intent();
                ProfileManager profileManager = ProfileManager.getInstance(this);

                for (VpnProfile profile : profileManager.getProfiles()) {
                    result.putExtra(VpnProfile.EXTRA_PROFILEUUID, profile.getUUID().toString());
                    break;
                }

                onActivityResult(VPNProfileList.IMPORT_PROFILE, RESULT_OK, result);
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        VpnStatus.addStateListener((VpnStatus.StateListener) MainActivity.this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume: Main activity resume called ");


//        if (getIntent() != null) {
//            String page = getIntent().getStringExtra("PAGE");
//            if ("graph".equals(page)) {
//                mPager.setCurrentItem(1);
//            }
//            setIntent(null);
//        }
        if (setList != null) {
            setList.clear();
        }
        Collection<VpnProfile> allvpn = ProfileManager.getInstance(MainActivity.this).getProfiles();
        setList.addAll(allvpn);



        for (VpnProfile profile : setList) {


            if (profile != null) {
                Log.i("checkprofilesn", "onResume: end loop just check list ............ " + profile.getName());
            }

        }

        Utils.setSelectedCountry(MainActivity.this, 0);
        // setList.addAll(allvpn);
        String duration = Utils.getSubscribeDuration(MainActivity.this);
        int index = Utils.getSelectedCountry(MainActivity.this);
        if (duration != null) {
            //   setListClickListeners(duration);
            if (index >= allvpn.size()) {
                Utils.currentVpnProfile = null;
                layout_country_bold.setVisibility(View.GONE);
            } else {

                Utils.currentVpnProfile = setList.get(index);
                //  setSelectedCountryTick(index, duration);
                //  layout_country_bold.setVisibility(View.VISIBLE);
            }
        } else {
            Utils.currentVpnProfile = null;
            layout_country_bold.setVisibility(View.GONE);
        }





    }


    private void inItViews() {


        interstitialAd_1 = new InterstitialAd(MainActivity.this);
        interstitialAd_2 = new InterstitialAd(MainActivity.this);

        interstitialAd_1.setAdUnitId(getResources().getString(R.string.inter1_id));
        interstitialAd_2.setAdUnitId(getResources().getString(R.string.inter2_id));

        inter1_Load();
        inter2_Load();


        adsListeners();

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Disconnecting ...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        layout_country_bold = findViewById(R.id.layout_county_bold);

        img_current_location = findViewById(R.id.img_current_location);
        txt_current_location = findViewById(R.id.txt_current_location);

        constraintLayout = findViewById(R.id.constrain_layout_new);
        tvStatus = findViewById(R.id.tvStatus);
        tv_device_status = findViewById(R.id.tv_device_status);


        tv_2 = findViewById(R.id.tv_2);
        tv_1 = findViewById(R.id.tv_1);


        statusDot = findViewById(R.id.status_dot);
        center_img = findViewById(R.id.center_img);
        connection_text_btn = findViewById(R.id.connection_text_btn);

        tv_device_status.setVisibility(View.GONE);
        tv_2.setVisibility(View.GONE);
        tv_1.setVisibility(View.GONE);


        linearLayoutHorizantal = findViewById(R.id.horizontal_linear_scroll);

        connection_text_btn.setOnClickListener(this);

    }

    private void adsListeners() {
        interstitialAd_1.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                inter1_load_check = false;
                inter1_Load();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                inter1_Load();
            }

            @Override
            public void onAdLoaded() {
                inter1_load_check = true;
            }
        });
        interstitialAd_2.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                inter2_load_check = false;
                inter2_Load();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                inter2_Load();
            }

            @Override
            public void onAdLoaded() {
                inter2_load_check = true;
            }
        });
    }


    private void inter1_Load() {
        interstitialAd_1.loadAd(new AdRequest.Builder().build());
    }

    private void inter2_Load() {
        interstitialAd_2.loadAd(new AdRequest.Builder().build());
    }

    private void pagerConfig() {

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager(), this);

        /* Toolbar and slider should have the same elevation */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            disableToolbarElevation();
        }


        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList.class);
        mPagerAdapter.addTab(R.string.graph, GraphFragment.class);

        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings.class);
        mPagerAdapter.addTab(R.string.faq, FaqFragment.class);

        if (SendDumpFragment.getLastestDump(this) != null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment.class);
        }


        if (isDirectToTV())
            mPagerAdapter.addTab(R.string.openvpn_log, LogFragment.class);

        mPagerAdapter.addTab(R.string.about, AboutFragment.class);
        mPager.setAdapter(mPagerAdapter);

        mTabs = (TabBarView) findViewById(R.id.sliding_tabs);
        mTabs.setViewPager(mPager);
    }


    private boolean isVpnConnected(VpnProfile profile) {
        return VpnStatus.isVPNActive() && profile.getUUIDString().equals(VpnStatus.getLastConnectedVPNProfile());
    }

    public void updateView(String status, Context context) {

        Log.i(TAG, "updateView: called for update .. " + status);

        if (statusDot != null && center_img != null && tvStatus != null) {


            switch (status) {
                case connected_view:

                    Log.i(TAG, "updateView: 1st conditon .. ");
                    ///  tv_1.setText(context.getResources().getString(R.string.tv_1_connect));
                    //   tv_2.setText(context.getResources().getString(R.string.tv_2_connect));
                    center_img.setImageResource(R.drawable.connected_ok);

                    tvStatus.setText("Connected");
                    statusDot.setImageResource(android.R.drawable.presence_online);

                    connection_text_btn.setText("DISCONNECT NOW");

                    // tvStatus.setGravity(Gravity.CENTER);
                    // tv_device_status.setText(context.getResources().getString(R.string.connected_text));

//                  Toast.makeText(context, "change views are is ", Toast.LENGTH_SHORT).show();

                    break;
                case disconnected_view:
                    Log.i(TAG, "updateView: 2st conditon .. ");
                    center_img.setImageResource(R.drawable.btn_go);
                    statusDot.setImageResource(android.R.drawable.ic_notification_overlay);

                    tvStatus.setText("Disconnected");
                    tvStatus.requestLayout();
                    connection_text_btn.setText("CONNECT NOW");
                    // tv_device_status.setText(context.getResources().getString(R.string.disconnected_text));
                    // tv_1.setText(context.getResources().getString(R.string.tv_1_disconnect));
                    // tv_2.setText(context.getResources().getString(R.string.tv_2_disconnect));
                    break;
                case connecting:
                    Log.i(TAG, "updateView: 3st conditon .. ");
                    center_img.setImageResource(R.drawable.btn_reload);
                    statusDot.setImageResource(R.drawable.dot_gray);

                    tvStatus.setText("Connecting");
                    tvStatus.requestLayout();
                    // tv_device_status.setText(context.getResources().getString(R.string.disconnected_text));
                    // tv_1.setText(context.getResources().getString(R.string.tv_1_disconnect));
                    // tv_2.setText(context.getResources().getString(R.string.tv_2_disconnect));
                    break;
            }

        }


    }


    private void setListClickListeners(String duration) {

        if (linearLayoutHorizantal.getChildCount() <= 0) {


            int counter = 0;

            if (duration.equals(Utils.SUBSCRIBE_THREE_MONTHS_TAG)) {

                counter = 0;

                for (String countryName : Utils.country_three_months_data.getCountries_names()) {
                    int countryFlage = Utils.country_three_months_data.getCountries_flgs()[counter];

                    View view = getCountryView(countryName, countryFlage);


                    view.setTag(String.valueOf(counter));
                    counter = counter + 1;

                    view.setOnClickListener(view1 -> {


                        if (!String.valueOf(Utils.getSelectedCountry(MainActivity.this)).equals(view1.getTag())) {

                            Utils.setSelectedCountry(MainActivity.this, Integer.valueOf((String) view1.getTag()));

                            //  Utils.currentVpnProfile = setList.get(Utils.getSelectedCountry(MainActivity.this));

                            if (Utils.currentVpnProfile != null) {
                                if (isVpnConnected(Utils.currentVpnProfile)) {
                                    if (progressDialog != null) {
                                        progressDialog.show();
                                    }
                                    new DisconnectVPN(MainActivity.this, DisconnectVPN.DISCONNECT_VPN_RESTART);
                                }
                            }

                            Utils.currentVpnProfile = setList.get(Integer.valueOf((String) view1.getTag()));
                            //  setSelectedCountryTick(Integer.valueOf((String) view.getTag()), duration);

                        } else {

                            Toast.makeText(MainActivity.this, "Your Connection Already at selected Country Location ", Toast.LENGTH_SHORT).show();
                        }

                        layout_country_bold.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "Location Changed", Toast.LENGTH_SHORT).show();


                    });
                    linearLayoutHorizantal.addView(view, getLinearLayoutPramas());
                }
            } else if (duration.equals(Utils.SUBSCRIBE_SIX_MONTHS_TAG)) {

                for (String countryName : Utils.country_six_months_data.getCountries_names()) {
                    int countryFlage = Utils.country_six_months_data.getCountries_flgs()[counter];
                    View view = getCountryView(countryName, countryFlage);


                    view.setTag(String.valueOf(counter));
                    counter = counter + 1;

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            //  Toast.makeText(MainActivity.this, "Location Changed", Toast.LENGTH_SHORT).show();

                            if (!String.valueOf(Utils.getSelectedCountry(MainActivity.this)).equals(view.getTag())) {


                                Utils.setSelectedCountry(MainActivity.this, Integer.valueOf((String) view.getTag()));

                                //  Utils.currentVpnProfile = setList.get(Utils.getSelectedCountry(MainActivity.this));

                                if (isVpnConnected(Utils.currentVpnProfile)) {
                                    if (progressDialog != null) {
                                        progressDialog.show();
                                    }
                                    new DisconnectVPN(MainActivity.this, DisconnectVPN.DISCONNECT_VPN_RESTART);
                                }

                                Utils.currentVpnProfile = setList.get(Integer.valueOf((String) view.getTag()));
                                //   setSelectedCountryTick(Integer.valueOf((String) view.getTag()), duration);

                            } else {
                                Toast.makeText(MainActivity.this, "Your Connection Already at selected Country Location ", Toast.LENGTH_SHORT).show();
                            }
                            layout_country_bold.setVisibility(View.VISIBLE);
                        }
                    });
                    linearLayoutHorizantal.addView(view, getLinearLayoutPramas());
                }

            } else if (duration.equals(Utils.SUBSCRIBE_twelve_Months_TAG)) {

                for (String countryName : Utils.country_twelve_months_data.getCountries_names()) {
                    int countryFlage = Utils.country_twelve_months_data.getCountries_flgs()[counter];
                    View view = getCountryView(countryName, countryFlage);


                    view.setTag(String.valueOf(counter));
                    counter = counter + 1;

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {


                            if (!String.valueOf(Utils.getSelectedCountry(MainActivity.this)).equals(view.getTag())) {


                                Utils.setSelectedCountry(MainActivity.this, Integer.valueOf((String) view.getTag()));

                                //  Utils.currentVpnProfile = setList.get(Utils.getSelectedCountry(MainActivity.this));

                                if (isVpnConnected(Utils.currentVpnProfile)) {
                                    if (progressDialog != null) {
                                        progressDialog.show();
                                    }
                                    new DisconnectVPN(MainActivity.this, DisconnectVPN.DISCONNECT_VPN_RESTART);
                                }
                                Utils.currentVpnProfile = setList.get(Integer.valueOf((String) view.getTag()));
                                //  setSelectedCountryTick(Integer.valueOf((String) view.getTag()), duration);

                            } else {
                                Toast.makeText(MainActivity.this, "Your Connection Already at selected Country Location ", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    linearLayoutHorizantal.addView(view, getLinearLayoutPramas());
                }
                layout_country_bold.setVisibility(View.VISIBLE);
            }


        }
    }


    private void setSelectedCountryTick(int index, String duration) {

        // selected tick are those ...............


        if (duration.equals(Utils.SUBSCRIBE_THREE_MONTHS_TAG)) {


            Log.i("checkindex", "setSelectedCountryTick: " + index);


            for (int i = 0; i < linearLayoutHorizantal.getChildCount(); i++) {

                ((RelativeLayout) linearLayoutHorizantal.getChildAt(i)).getChildAt(1).setVisibility(View.INVISIBLE);
            }

            ((RelativeLayout) linearLayoutHorizantal.getChildAt(index)).getChildAt(1).setVisibility(View.VISIBLE);

            img_current_location.setImageResource((Utils.country_three_months_data.getCountries_flgs()[index]));
            txt_current_location.setText((Utils.country_three_months_data.getCountries_names()[index]));


        } else if (duration.equals(Utils.SUBSCRIBE_SIX_MONTHS_TAG)) {

            for (int i = 0; i < linearLayoutHorizantal.getChildCount(); i++) {

                ((RelativeLayout) linearLayoutHorizantal.getChildAt(i)).getChildAt(1).setVisibility(View.INVISIBLE);
            }

            ((RelativeLayout) linearLayoutHorizantal.getChildAt(index)).getChildAt(1).setVisibility(View.VISIBLE);

            img_current_location.setImageResource((Utils.country_six_months_data.getCountries_flgs()[index]));
            txt_current_location.setText((Utils.country_six_months_data.getCountries_names()[index]));


        } else if (duration.equals(Utils.SUBSCRIBE_twelve_Months_TAG)) {

            for (int i = 0; i < linearLayoutHorizantal.getChildCount(); i++) {

                ((RelativeLayout) linearLayoutHorizantal.getChildAt(i)).getChildAt(1).setVisibility(View.INVISIBLE);
            }

            ((RelativeLayout) linearLayoutHorizantal.getChildAt(index)).getChildAt(1).setVisibility(View.VISIBLE);

            img_current_location.setImageResource((Utils.country_twelve_months_data.getCountries_flgs()[index]));
            txt_current_location.setText((Utils.country_twelve_months_data.getCountries_names()[index]));


        }

        try {
            Utils.currentVpnProfile = setList.get(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            Utils.currentVpnProfile = null;
        }


    }

    private View getCountryView(String title, int id) {

        View view = LayoutInflater.from(this).inflate(R.layout.flag_layout, null, false);
        ((ImageView) ((ViewGroup) view).findViewById(R.id.flag_img)).setImageResource(id);
        return view;
    }


    private LinearLayout.LayoutParams getLinearLayoutPramas() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.leftMargin = 5;
        layoutParams.rightMargin = 5;
        layoutParams.topMargin = 10;
        layoutParams.bottomMargin = 10;
        return layoutParams;
    }


    private boolean isDirectToTV() {
        return (getPackageManager().hasSystemFeature(FEATURE_TELEVISION)
                || getPackageManager().hasSystemFeature(FEATURE_LEANBACK));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void disableToolbarElevation() {
//        ActionBar toolbar = getActionBar();
//        toolbar.setElevation(0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_log) {
            Intent showLog = new Intent(this, LogWindow.class);
            startActivity(showLog);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        if (requestCode == VPNProfileList.IMPORT_PROFILE) {

            // send profile to main activity other view hide completely

            if (Utils.CURRENT_MONTHS.equals(Utils.SUBSCRIBE_THREE_MONTHS_TAG)) {


                String profileUUID = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);
                VpnProfile vpnProfile = ProfileManager.get(MainActivity.this, profileUUID);
                Utils.setSubscribeDuration(MainActivity.this, true, Utils.SUBSCRIBE_THREE_MONTHS_TAG);
                Utils.currentVpnProfile = vpnProfile;
                setList.addAll(ProfileManager.getInstance(this).getProfiles());

                //  Utils.setSelectedCountry(SubscriptionActivity.this,0);

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                vpnConfig();


            } else if (Utils.CURRENT_MONTHS.equals(Utils.SUBSCRIBE_SIX_MONTHS_TAG)) {

                Toast.makeText(this, "not defined yet", Toast.LENGTH_SHORT).show();

            } else if (Utils.CURRENT_MONTHS.equals(Utils.SUBSCRIBE_twelve_Months_TAG)) {

                Toast.makeText(this, "not defined yet", Toast.LENGTH_SHORT).show();

            }


        }

        System.out.println(data);


    }

    public void callOnClickFromVPNProfileList() {
        onClick(connection_text_btn);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.more_options:
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
                popupMenu.inflate(R.menu.vpn_main_menu);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {

                            case R.id.allow_apps_id:
                                if (Utils.currentVpnProfile == null) {
                                    Toast.makeText(MainActivity.this, "Location not selected", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                                showFragment(SHOW_FRAGMENT_APPS);

                                break;

                            case R.id.data_usage_id:
                                if (inter2_load_check) {
                                    interstitialAd_1.show();
                                }
                                showFragment(SHOW_FRAGMENT_GRPH);

                                break;

//                            case R.id.update_subscription_id:
//
//                                Intent intent = new Intent(MainActivity.this, SubscriptionActivity.class);
//                                intent.putExtra(SubscriptionActivity.UPDATE_SUBSCRIPTION, true);
//                                startActivity(intent);
//                                break;

                        }

                        return true;

                    }
                });
                popupMenu.show();
                break;

            case R.id.connection_text_btn:

                if (Utils.currentVpnProfile != null) {

                    if (isVpnConnected(Utils.currentVpnProfile)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.title_cancel);
                        builder.setView(ad);
                        builder.setMessage(R.string.cancel_connection_query);
                        builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.setPositiveButton(R.string.cancel_connection, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (progressDialog != null) {
                                    progressDialog.show();
                                }

//                                Intent disconnectVPN = new Intent(MainActivity.this, DisconnectVPN.class);
//                                disconnectVPN.putExtra(DisconnectVPN.DISCONNECT_VPN_TAG,DisconnectVPN.DISCONNECT_VPN_BTN);
//                                startActivity(disconnectVPN);

                                new DisconnectVPN(MainActivity.this, DisconnectVPN.DISCONNECT_VPN_BTN);

                            }
                        });

                        //  builder.setNeutralButton(R.string.reconnect, this);

                        builder.show();

                    } else {
                        startVPN(Utils.currentVpnProfile);
                    }
                } else {
                    Toast.makeText(this, "Please select One Country Location", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.switch_view:
                if (constraintLayout.getVisibility() == View.VISIBLE) {
                    switch_view(TABS_VIEW);
                } else {
                    switch_view(CONSTRAINT_VIEW);
                }
                break;

            default:
                break;

        }
    }

    private void showFragment(String fragment) {
        Intent intent = new Intent(MainActivity.this, FragmentShowMainActivity.class);
        intent.putExtra(SHOW_FRAGMENT_TAG, fragment);
        startActivity(intent);

    }

    private void switch_view(String view_type) {


        if (mPager == null || mSlidingTabLayout == null) {

            mPager = (ViewPager) findViewById(R.id.pager);
            mTabs = (TabBarView) findViewById(R.id.sliding_tabs);
        }


        if (view_type.equals(CONSTRAINT_VIEW)) {

            constraintLayout.setVisibility(View.VISIBLE);
            ((TabBarView) mTabs).setVisible(View.GONE);
            mPager.setVisibility(View.GONE);


        } else if (view_type.equals(TABS_VIEW)) {

            constraintLayout.setVisibility(View.GONE);
            ((TabBarView) mTabs).setVisible(View.VISIBLE);
            mPager.setVisibility(View.VISIBLE);


        }
    }

    private void startVPN(VpnProfile profile) {

        if (Utils.isNetworkConnected(MainActivity.this)) {
            tvStatus.setText("Connecting");
            //  tvStatus.setGravity(Gravity.CENTER);
            ProfileManager.getInstance(MainActivity.this).saveProfile(MainActivity.this, profile);
            Intent intent = new Intent(MainActivity.this, LaunchVPN.class);
            intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
            intent.setAction(Intent.ACTION_MAIN);
            startActivity(intent);
        } else {
            Toast.makeText(this, "network not connected", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public void onVpnDisconnect(boolean isReconnect) {
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
        updateView(disconnected_view, MainActivity.this);
        if (isReconnect) {
            startVPN(Utils.currentVpnProfile);
            tvStatus.setText("ReConnecting");
        }

    }


    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {

        Log.i(TAG, "updateState: state ... " + state + " ... logmessage .. " + logmessage + " ... localizedResId ... " + localizedResId);

        switch (localizedResId) {

            case R.string.state_connecting:
                tvStatus.setText(connecting);
                Utils.setCurrentStatus(MainActivity.this, connecting);
                Log.i(TAG, "updateState: connecting");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateView(connecting, MainActivity.this);
                            }
                        });
                    }
                }).start();

                break;
            case R.string.state_wait:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.setCurrentStatus(MainActivity.this, connecting);
                                updateView(connecting, MainActivity.this);
                            }
                        });
                    }
                }).start();

                tvStatus.setText(connecting);
                Log.i(TAG, "updateState: sever reply");

                break;
            case R.string.state_auth:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.setCurrentStatus(MainActivity.this, connecting);
                                updateView(connecting, MainActivity.this);
                            }
                        });
                    }
                }).start();

                tvStatus.setText(connecting);
                Log.i(TAG, "updateState: sever auth");

                break;
            case R.string.state_get_config:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.setCurrentStatus(MainActivity.this, connecting);
                                updateView(connecting, MainActivity.this);
                            }
                        });
                    }
                }).start();

                tvStatus.setText(connecting);
                Log.i(TAG, "updateState: sever config");
                break;
            case R.string.state_assign_ip:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.setCurrentStatus(MainActivity.this, connecting);
                                updateView(connecting, MainActivity.this);
                            }
                        });
                    }
                }).start();

                Log.i(TAG, "updateState: sever assign ip adress");

                break;
            case R.string.state_add_routes:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.setCurrentStatus(MainActivity.this, connecting);
                                updateView(connecting, MainActivity.this);
                            }
                        });
                    }
                }).start();


                Log.i(TAG, "updateState: sever routes");
                break;
            case R.string.state_connected:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvStatus.setText("Connected");
                                updateView(connected_view, MainActivity.this);
                                if (interstitialAd_2.isLoaded()) {
                                    interstitialAd_2.show();
                                } else {
                                    inter2_Load();
                                }
                            }
                        });
                    }
                }).start();

                Log.i(TAG, "updateState: sever connected");
                //updateView(connected_view);
                Utils.setCurrentStatus(MainActivity.this, other);

                tv_1.setText(getResources().getString(R.string.tv_1_connect));
                tv_2.setText(getResources().getString(R.string.tv_2_connect));
                center_img.setImageResource(R.drawable.connected_ok);


                break;
            case R.string.state_disconnected:
                new Thread(() -> runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "try later, some error exit", Toast.LENGTH_SHORT).show();
                    updateView(disconnected_view, MainActivity.this);
                })).start();

                Utils.setCurrentStatus(MainActivity.this, other);
                Log.i(TAG, "updateState: sever disconnected");

                break;
            case R.string.state_reconnecting:
                new Thread(() -> runOnUiThread(() -> {
                    Utils.setCurrentStatus(MainActivity.this, connecting);
                    tvStatus.setGravity(Gravity.CENTER);
                    updateView(connecting, MainActivity.this);
                })).start();

                Log.i(TAG, "updateState: sever reconnecting");
                break;
            case R.string.state_exiting:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "try later, some error exit", Toast.LENGTH_SHORT).show();
                                updateView(disconnected_view, MainActivity.this);
                            }
                        });
                    }
                }).start();

                Utils.setCurrentStatus(MainActivity.this, other);
                Log.i(TAG, "updateState: sever exiting");
                break;

            default:
                break;
        }
        if (logmessage.equals(R.string.state_noprocess)) {

            Utils.setCurrentStatus(MainActivity.this, other);

            Toast.makeText(this, "no process running ", Toast.LENGTH_SHORT).show();
            updateView(disconnected_view, MainActivity.this);
        }
    }

    @Override
    public void setConnectedVPN(String uuid) {

    }

    @Override
    public void onAppsListChanged() {
        if (isVpnConnected(Utils.currentVpnProfile)) {

            if (progressDialog != null) {
                progressDialog.show();
            }

//            Intent disconnectVPN = new Intent(MainActivity.this, DisconnectVPN.class);
//            disconnectVPN.putExtra(DisconnectVPN.DISCONNECT_VPN_TAG,DisconnectVPN.DISCONNECT_VPN_RESTART_APPS_REFRESH);
//            startActivity(disconnectVPN);
            new DisconnectVPN(MainActivity.this, DisconnectVPN.DISCONNECT_VPN_RESTART_APPS_REFRESH);

        }
    }

    @Override
    public void lunchCancel() {
        Toast.makeText(this, "Allow app permission to start vpn MyDissconnectService", Toast.LENGTH_SHORT).show();
        updateView(disconnected_view, MainActivity.this);
    }
}
