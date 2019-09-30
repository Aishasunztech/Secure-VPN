package de.blinkt.openvpn.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import de.blinkt.openvpn.R;

public class GetStartedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getWindow().getDecorView();
        int visiblility = WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        view.setSystemUiVisibility(visiblility);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_get_started);
        if(getActionBar()!=null){
            getActionBar().hide();
        }
    }

    public void getStartedBtn(View view) {

        Intent intent = new Intent(GetStartedActivity.this,MainActivity.class);
        startActivity(intent);
        finish();

    }
}
