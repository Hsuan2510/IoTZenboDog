package com.example.zenbocontrol;

import static android.Manifest.permission.*;
import static com.asus.robotframework.API.MotionControl.SpeedLevel.Head.L1;
import static com.asus.robotframework.API.MotionControl.SpeedLevel.Head.L3;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotCommand;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.Utility;
import com.asus.robotframework.API.VisionConfig;
import com.asus.robotframework.API.WheelLights;
import com.asus.robotframework.API.MotionControl;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import kotlinx.coroutines.Delay;


public class MainActivity extends RobotActivity {
    private static String speechSubscriptionKey = "98a147ddcd8847ed80aa85fac2f85801";
    // Replace below with your own service region (e.g., "westus").
    private static String serviceRegion = "eastasia";
    public static final int TYPE_CAPACITY_TOUCH = Utility.SensorType.CAPACITY_TOUCH;
    // Socket components
    //private static String serverIP = "0.0.0.0";  // zenbo IP
    private static int serverPort = 7777;
    private static ServerSocket serverSocket;   // Socket on server side
    private ArrayList clientList = new ArrayList();
    private static int count = 0;   // counting Client connection number
    private Thread thread;
    int exprCount=0;
    int lightCount=0;
    int bCount = 0;
    int hCount = 0;
    int tCount = 0;
    String speechcatch="";
    TextView zenboIP;
    TextView expressionShow;
    TextView wheelLightShow;

    TextView textViewTest ;

    public MainActivity(RobotCallback robotCallback, RobotCallback.Listen robotListenCallback){
        super(robotCallback, robotListenCallback);
    }

    // sensor manager
    private SensorManager mSensorManager;
    // sensor
    private Sensor mSensorCapacityTouch;
    private TextView mTextView_capacity_touch_value0;   //按了幾秒
    private TextView mTextView_capacity_touch_value1;   //按了幾次

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //speech
        int requestCode = 5; // unique code for the permission request
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);
        //new line
        this.robotAPI=new RobotAPI(getApplicationContext(),robotCallback);
        mTextView_capacity_touch_value0 = (TextView)findViewById(R.id.id_sensor_type_capacity_touch_value0_value);
        mTextView_capacity_touch_value1 = (TextView)findViewById(R.id.id_sensor_type_capacity_touch_value1_value);

        // sensor manager
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorCapacityTouch = mSensorManager.getDefaultSensor(TYPE_CAPACITY_TOUCH);
        mSensorManager.registerListener(listenerCapacityTouch, mSensorCapacityTouch, SensorManager.SENSOR_DELAY_UI);
        
        initViewElement();
        String serverIP = getLocalIpAddress();
        //title
        String str = "Zenbo IP:" + serverIP + "\nPort" + serverPort;
        zenboIP.setText(str);
        try {
            startSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //robotAPI.robot.setExpression(RobotFace.DEFAULT);
        //robotAPI.robot.setExpression(RobotFace.DOUBTING);
    }

    private void initViewElement() {
        zenboIP = (TextView)findViewById(R.id.zenboIP);
        expressionShow = (TextView) findViewById(R.id.expressionShow);
        wheelLightShow = (TextView) findViewById(R.id.wheelLightShow);

        textViewTest = (TextView) findViewById(R.id.textViewTest);
    }

    //speech
    public void Speech() {
        try (SpeechConfig config = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
             SpeechRecognizer reco = new SpeechRecognizer(config);) {

            Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();

            // Note: this will block the UI thread, so eventually, you want to
            //       register for the event (see full samples)
            SpeechRecognitionResult result = task.get();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
               speechcatch=result.toString();
               speechcatch.toUpperCase();
                runOnUiThread( () -> {
                    zenboIP.append(speechcatch);
                });
                if(speechcatch.contains("weather")||speechcatch.contains("Weather")){
                    getData("myzenbo");
                }
                else if(speechcatch.contains("follow")||speechcatch.contains("Follow")){
                    robotAPI.robot.setExpression(RobotFace.ACTIVE);
                    robotAPI.robot.speak("I'm coming!");
                    robotAPI.utility.followUser();
                }
                else if(speechcatch.contains("I need you")){
                    robotAPI.robot.setExpression(RobotFace.SHY);
                }
                else if(speechcatch.contains("stop")||speechcatch.contains("Stop")){
                    robotAPI.motion.stopMoving();
                    robotAPI.cancelCommandAll();
                }
                else if(speechcatch.contains("go")||speechcatch.contains("Go")){
                    robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.FORWARD);
                    robotAPI.robot.speak("I'm forward");
                }
                else if (speechcatch.contains("back")||speechcatch.contains("Back")){
                    robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.BACKWARD);
                    robotAPI.robot.speak("I'm back");
                }
                else if(speechcatch.contains("Humidity")||speechcatch.contains("humidity")){
                    getDataH("myzenbo2");
                }
            }
            else {
                robotAPI.robot.speak("I don't understand");
            }

        } catch (Exception ex) {
            Log.e("SpeechSDKDemo", "unexpected " + ex.getMessage());
            assert(false);
        }
    }


    void getDataH(final String param){
        HttpURLConnection connection;
        try {
            runOnUiThread( () -> {
                // textViewTest.append("123");
            });
            URL url = new URL("http://192.168.0.33:3000/api/devices/BJDwYzlus/data" +
                    "channels/"+param+"/datapoints");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("deviceKey", "454b60eb99eb165cee50fb1114e005857d2567ae399a64d65b30c607d21384ed");
            connection.setDoInput(true);

            InputStream inputStream = connection.getInputStream();


            BufferedReader bR = new BufferedReader(  new InputStreamReader(inputStream));
            String line = "";


            StringBuilder responseStrBuilder = new StringBuilder();

            while((line =  bR.readLine()) != null){
                responseStrBuilder.append(line);
            }
            inputStream.close();
            runOnUiThread( () -> {
                textViewTest.append("456");
            });
            JSONObject jsonPost = new JSONObject(responseStrBuilder.toString());

            // get the 7697 temperature data from sandbox
            JSONObject object = (JSONObject) jsonPost.getJSONArray("data").get(0);
            String str = object.getJSONObject("values").getString("value");

            robotAPI.robot.setExpression(RobotFace.HAPPY, "The humidity now is "+str+" percent ");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //listener - TYPE_CAPACITY_TOUCH
    SensorEventListener listenerCapacityTouch = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mTextView_capacity_touch_value0.setText(String.valueOf(event.values[0]));   //event.value[0] 按了幾秒
            mTextView_capacity_touch_value1.setText(String.valueOf(event.values[1]));   //event.value[1] 按了幾次
            if(event.values[0]>=1){
                robotAPI.robot.speak("Wolff-ff!，How can I help you?");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Speech();
                    }
                }).start();
            }

        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    void getData(final String param){
        HttpURLConnection connection;
        try {
            runOnUiThread( () -> {
                // textViewTest.append("123");
            });
            URL url = new URL("http://192.168.0.33:3000/api/devices/BJDwYzlus/data" +
                    "channels/"+param+"/datapoints");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("deviceKey", "454b60eb99eb165cee50fb1114e005857d2567ae399a64d65b30c607d21384ed");
            connection.setDoInput(true);


            runOnUiThread( () -> {
                textViewTest.append("Before InputStream");
            });
            InputStream inputStream = connection.getInputStream();

            runOnUiThread( () -> {
                textViewTest.append("Line1");
            });

            BufferedReader bR = new BufferedReader(  new InputStreamReader(inputStream));
            String line = "";


            StringBuilder responseStrBuilder = new StringBuilder();

            runOnUiThread( () -> {
                textViewTest.append("Before while");
            });

            while((line =  bR.readLine()) != null){
                responseStrBuilder.append(line);
            }
            inputStream.close();
            runOnUiThread( () -> {
                textViewTest.append("456");
            });
            JSONObject jsonPost = new JSONObject(responseStrBuilder.toString());

            // get the 7697 temperature data from sandbox
            JSONObject object = (JSONObject) jsonPost.getJSONArray("data").get(0);
            String str = object.getJSONObject("values").getString("value");

            robotAPI.robot.setExpression(RobotFace.HAPPY, "The weather today "+str+" degree celcius");
            runOnUiThread( () -> {
                textViewTest.append("789");
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static RobotCallback robotCallback = new RobotCallback() {
        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);

            Log.d("RobotDevSample", "onResult:"
                    + RobotCommand.getRobotCommand(cmd).name()
                    + ", serial:" + serial + ", err_code:" + err_code
                    + ", result:" + result.getString("RESULT"));
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
        }

        @Override
        public void initComplete() {
            super.initComplete();

        }
    };

    public static RobotCallback.Listen robotListenCallback = new RobotCallback.Listen() {
        @Override
        public void onFinishRegister() {
        }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) {
        }

        @Override
        public void onSpeakComplete(String s, String s1) {
        }

        @Override
        public void onEventUserUtterance(JSONObject jsonObject) {
        }

        @Override
        public void onResult(JSONObject jsonObject) {
        }

        @Override
        public void onRetry(JSONObject jsonObject) {
        }
    };

    public MainActivity(){
        super(robotCallback, robotListenCallback);

    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return null;
    }

    // start the socket server
    private void startSocket() throws IOException {
        serverSocket = new ServerSocket(serverPort);

        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!serverSocket.isClosed()) {
                    waitNewClient();
                    System.out.println("Waiting...\n");
                }
            }
        });
        clientThread.start();
    }

    // waiting for client connection
    private void waitNewClient() {
        try {
            Socket socket = serverSocket.accept();
            ++count;
            addNewClient(socket);   // add new client
        } catch (IOException e) {
            e.getStackTrace();
        }
    }

    // add new client
    public void addNewClient(Socket socket) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientList.add(socket);    // add new client
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream())); // fetch network stream

                    // when socket connected
                    while(socket.isConnected()) {
                        runOnUiThread( () -> {
                            zenboIP.append("\nclient Connected");
                        });

                        String readMsg = br.readLine();     // fetch network stream message

                        if(readMsg!=null) {
                            runOnUiThread( () -> {
                                zenboIP.setText(readMsg);
                            });
                            new Thread(() -> {
                                JSONObject readObj = null;
                                try {
                                    readObj = new JSONObject(readMsg);

                                    String type = readObj.getString("Type");
                                    String msg = readObj.getString("MSG");
                                    if(msg.equals("1")){
                                        robotAPI.robot.speak(type);
                                    }
                                    if(type.equals("track")){
                                        robotAPI.utility.trackUser();
                                    }
                                    else if (type.equals("follow")){
                                        robotAPI.utility.followUser();
                                    }
                                    else if (type.equals("stop")){
                                        robotAPI.motion.stopMoving();
                                        robotAPI.cancelCommandAll();
                                        //robotAPI.utility.resetToDefaultSetting();
                                    }
                                    else if(type.equals("detect")){
                                        int intervalInMS = 0,timeoutInMS = 0;
                                        robotAPI.vision.requestDetectFace(new VisionConfig.FaceDetectConfig());
                                        robotAPI.vision.requestDetectPerson(intervalInMS);
                                        robotAPI.vision.requestGesturePoint(timeoutInMS);
                                    }
                                    else if(type.equals("open")){
                                        robotAPI.robot.speak("Someone is coming!");
                                    }
                                    /*
                                    if(type.equals("expression")) {
                                        exprCount++;
                                        exprCount = exprCount %3;
                                        expressionChange(exprCount);
                                    }
                                    else if(type.equals("moveBody")) {
                                        bCount++;
                                        bCount = bCount %4;
                                        moveBody(bCount);
                                    }
                                    else if(type.equals("moveHead")) {
                                        hCount++;
                                        hCount = hCount %4;
                                        moveHead(hCount);
                                    }
                                    else if(type.equals("talk")) {
                                        tCount++;
                                        tCount = tCount %4;
                                        talk(tCount);
                                    }
                                    else if(type.equals("look")) {
                                        robotAPI.utility.lookAtUser(0);
                                    }

                                     */

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // remove client
                    clientList.remove(socket);
                    --count;
                }
            }
        });
        t.start();      // start thread
    }


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

    public void moveBody(int Count) {
        if(Count==1) {
            float x = (float) 1.2;
            float y = (float) 0.8;
            float theta = (float) 1.57;
            robotAPI.motion.moveBody(x, y, theta);
        }
        else if(Count==2) {
            float x = (float) -1.2;
            float y = (float) -0.8;
            float theta = (float) 1.57;
            robotAPI.motion.moveBody(x, y, theta);
        }
        else {
            float x = (float) 1.2;
            float y = (float) -0.8;
            float theta = (float) 1.57;
            robotAPI.motion.moveBody(x, y, theta);
        }
    }

    public void moveHead(int Count) {
        if(Count==1) {
            float yaw = (float) -0.52;
            float pitch = (float) 0.26;
            MotionControl.SpeedLevel.Head level = L1;
            robotAPI.motion.moveHead(yaw, pitch, level);
        }
        else if(Count==2) {
            float yaw = (float) -0.52;
            float pitch = (float) 0.26;
            MotionControl.SpeedLevel.Head level = L3;
            robotAPI.motion.moveHead(yaw, pitch, level);
        }
        else {
            float yaw = (float) -0.52;
            float pitch = (float) 0.26;
            MotionControl.SpeedLevel.Head level = L1;
            robotAPI.motion.moveHead(yaw, pitch, level);
        }
    }

    public void talk(int Count) {
        if(Count==1) {
            robotAPI.motion.stopMoving();
            robotAPI.robot.speak("Hello");
        }
        else if(Count==2) {
            robotAPI.motion.stopMoving();
            robotAPI.robot.speak("I am Zenbo.");
        }
        else if(Count==3){
            robotAPI.motion.stopMoving();
            robotAPI.robot.speak("Nice to meet you.");
            //just want to try
        }
        else {
            robotAPI.motion.stopMoving();
            robotAPI.robot.speak("HaHa");
        }
    }
}