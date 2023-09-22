package com.example.zenbocontrol;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotFace;

public class RobotActivity extends Activity {
    public RobotAPI robotAPI;
    RobotCallback robotCallback;
    RobotCallback.Listen robotListenCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.robotAPI = new RobotAPI(getApplicationContext(), robotCallback);
        robotAPI.robot.setExpression(RobotFace.PROUD);
    }

    public RobotActivity (RobotCallback robotCallback, RobotCallback.Listen robotListenCallback) {
        this.robotCallback = robotCallback;
        this.robotListenCallback = robotListenCallback;
    }

    @Override
    protected void onPause() {
        super.onPause();
        robotAPI.robot.unregisterListenCallback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(robotListenCallback!= null)
            robotAPI.robot.registerListenCallback(robotListenCallback);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        robotAPI.release();
    }
    /*
    public void expressionChange(int count) {
        if(count==1) {
            robotAPI.robot.setExpression(RobotFace.INTERESTED);
        }
        else if(count==2) {
            robotAPI.robot.setExpression(RobotFace.DOUBTING);
        }
        else if(count==3) {
            robotAPI.robot.setExpression(RobotFace.PROUD);
        }
        else {
            robotAPI.robot.setExpression(RobotFace.DEFAULT);
        }
    }

    public void wheelChange(int Count) {
        if(Count==1) {
            //.startBlinking
            robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
            robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xff, 0x007F7F);
            robotAPI.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 10);
            robotAPI.wheelLights.startBlinking(WheelLights.Lights.SYNC_BOTH, 0xff, 30, 10, 5);
        }
        else if(Count==2) {
            //.startBreathing
            robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
            robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xff, 0x00D031);
            robotAPI.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 10);
            robotAPI.wheelLights.startBreathing(WheelLights.Lights.SYNC_BOTH, 0xff, 20, 10, 0);
        }
        else {
            //.startMarquee
            robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
            robotAPI.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 20);
            robotAPI.wheelLights.startMarquee(WheelLights.Lights.SYNC_BOTH, WheelLights.Direction.DIRECTION_FORWARD, 40, 20, 3);
        }
    }
     */
}
