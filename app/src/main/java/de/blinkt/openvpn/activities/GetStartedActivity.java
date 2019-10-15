package de.blinkt.openvpn.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import de.blinkt.openvpn.R;

import static de.blinkt.openvpn.Utils.TOUR_STATUS;

public class GetStartedActivity extends Activity {
    private SharedPreferences sharedPreferences ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);
        View view = getWindow().getDecorView();
        int visibility = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        view.setSystemUiVisibility(visibility);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_get_started);
        if(getActionBar()!=null){
            getActionBar().hide();
        }
    }

    public void getStartedBtn(View view) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(TOUR_STATUS,true);
        editor.apply();

        Intent intent = new Intent(GetStartedActivity.this,MainActivity.class);
        startActivity(intent);
        finish();

    }
}
