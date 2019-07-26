package com.sss.simpleDrawerLayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private SimpleDrawerLayout simpleDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        simpleDrawerLayout = findViewById(R.id.simpleDrawerLayout);
        simpleDrawerLayout.setOnSimpleDrawerLayoutCallBack(new SimpleDrawerLayout.OnSimpleDrawerLayoutCallBack() {
            @Override
            public void onDrawerStatusChanged(SimpleDrawerLayout drawerLayout, int status, int amount, int state) {
//                Log.e("SSS", status +  "---" + amount + "---" + state);
            }
        });
    }

    float percent = 0.7f;

    public void percent(View view) {
        percent = percent == 0.7f ? 0.5f : 0.7f;
        simpleDrawerLayout.setDrawerPercent(percent);
    }


    public void fromRight(View view) {
        simpleDrawerLayout.setGravity(Gravity.RIGHT);
        go();
    }

    public void fromLeft(View view) {
        simpleDrawerLayout.setGravity(Gravity.LEFT);
        go();
    }

    public void fromTop(View view) {
        simpleDrawerLayout.setGravity(Gravity.TOP);
        go();
    }

    public void fromBottom(View view) {
        simpleDrawerLayout.setGravity(Gravity.BOTTOM);
        go();
    }

    private void go() {
        if (simpleDrawerLayout.getDrawerStatus() == SimpleDrawerLayout.CLOSED) {
            simpleDrawerLayout.openDrawer(simpleDrawerLayout.getGravity());
        } else {
            simpleDrawerLayout.closeDrawers();
        }
    }

    Random random=new Random();
    public void color(View view) {
        simpleDrawerLayout.setBesselColor(random.nextInt(256),random.nextInt(256),random.nextInt(256),random.nextInt(256));
    }

    public void drawerColor(View view) {
        simpleDrawerLayout.setBackgroundColor(getResources().getColor(R.color.colorAccent));
    }
}
