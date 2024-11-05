package com.tripod.testdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gwsd.GWPttApi;
import com.gwsd.GWPttEngine;
import com.gwsd.GWVideoEngine;
import com.gwsd.bean.GWMsgBean;
import com.gwsd.bean.GWMsgResponseBean;
import com.gwsd.bean.GWType;
import com.gwsd.rtc.view.GWRtcSurfaceVideoRender;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GW_MainActivity";

    Button btnLogin;
    Button btnQueryGrp;
    Button btnSwitchGrp;
    Button btnQueryMembers;
    Button btnLogout;
    Button btnReqSpk;
    Button btnRelSpk;
    Button btnTempGrp;
    Button btnStartMsg;
    Button btnSendMsg;
    Button btnGetWeather;

    Button btnVideo;
    Button btnVideoCall;
    Button btnVideoAccept;
    Button btnVideoHandup;

    EditText editAccount;
    EditText editPassword;

    GWRtcSurfaceVideoRender gwRtcSurfaceVideoRenderLocal;
    GWRtcSurfaceVideoRender gwRtcSurfaceVideoRenderRemote;


    Disposable disposable;

    GWPttEngine pttManager;
    GWVideoEngine videoEngine;
    boolean gsilent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();
        initEvent();
        Log.d(TAG, "onCreate="+pttManager);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart="+pttManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume="+pttManager);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    GWPttApi.GWPttObserver pttObserver = new GWPttApi.GWPttObserver() {
        @Override
        public void onPttEvent(int event, String data, int i1) {
            Log.d(TAG, "ptt event=" + event + " data=" + data);
            if (event == 0) {
                startTimer();
            } else if (event == 8 || event == -1) {
                stopTimer();
            }
        }

        @Override
        public void onMsgEvent(int status, String data) {
            Log.d(TAG, "msg status=" + status + " data=" + data);
            if (status == GWType.GW_MSG_STATUS.GW_MSG_STATUS_DATA) {
                GWMsgBean gwMsgBean = JSON.parseObject(data, GWMsgBean.class);
                Log.d(TAG, "msg content=" + gwMsgBean.toString());
            } else if (status == GWType.GW_MSG_STATUS.GW_MSG_STATUS_WEATHER) {
                GWMsgResponseBean gwMsgResponseBean = JSON.parseObject(data, GWMsgResponseBean.class);
                GWMsgResponseBean.Weather weather = JSON.parseObject(JSONObject.toJSONString(gwMsgResponseBean.getData()), GWMsgResponseBean.Weather.class);
                Log.d(TAG, "wea=" + weather.getWeath());
            } else if (status == GWType.GW_MSG_STATUS.GW_MSG_STATUS_ADDRESS) {
                GWMsgResponseBean gwMsgResponseBean = JSON.parseObject(data, GWMsgResponseBean.class);
                GWMsgResponseBean.Address address = JSON.parseObject(JSONObject.toJSONString(gwMsgResponseBean.getData()), GWMsgResponseBean.Address.class);
                Log.d(TAG, "ads=" + address.getAds());
            }
        }

        @Override
        public void onVideoEvent(String data) {

        }
    };

    GWVideoEngine.GWVideoEventHandler videoEventHandler = new GWVideoEngine.GWVideoEventHandler(){
        @Override
        public void onVideoPull(String remoteId, String remoteNm, int userType, boolean silent) {
            runOnUiThread(()->{
                if (!silent) {
                    String data = "recv " + remoteNm + " video pull request";
                    Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                } else {
                    gsilent = silent;
                    videoEngine.videoAcceptPull("gwsd03", "5372355", "公网时代_三号", 2, false);
                }
            });
        }

        @Override
        public void onVideoCall(String remoteId, String remoteNm) {
            runOnUiThread(()->{
                String data = "recv "+remoteNm+" video call request";
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onVideoMeetingInvite(String creater, String topic) {
            runOnUiThread(()->{
                String data = "recv "+creater+" video meeting request";
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onVideoMeetingCancel() {
            runOnUiThread(()->{
                String data = "video meeting cancel";
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onVideoMeetingSpeak() {

        }

        @Override
        public void onVideoMeetingMute() {

        }

        @Override
        public void onVideoMeetingUserJoin(long videoId, String id, String name, boolean video) {
            runOnUiThread(()->{
                String data = name+ "join meeting"+" "+video;
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                if (video) {
                    videoEngine.videoAttachRemoteView(gwRtcSurfaceVideoRenderRemote, videoId);
                }
            });
        }

        @Override
        public void onVideoMeetingSelfJoin() {
            runOnUiThread(()->{
                String data = "I join meeting";
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                videoEngine.videoAttachLocalView(gwRtcSurfaceVideoRenderLocal);
            });
        }

        @Override
        public void onVideoMeetingUserLeave(long videoId) {

        }

        @Override
        public void onVideoMeetingKickout() {

        }

        @Override
        public void onLocalStreamReady() {
            runOnUiThread(()->{
                if (gsilent) {

                } else {
                    Log.i(TAG, "add local view");
                    videoEngine.videoAttachLocalView(gwRtcSurfaceVideoRenderLocal);
                }
            });
        }

        @Override
        public void onRemoteStreamReady(boolean video, long userid) {
            runOnUiThread(()->{
                Log.i(TAG, "add remote view");
                videoEngine.videoAttachRemoteView(gwRtcSurfaceVideoRenderRemote, userid);
            });
        }

        @Override
        public void onRemoteStreamRemove() {
            runOnUiThread(()->{
                //videoEngine.videoDetachRemoteView(gwRtcSurfaceVideoRenderRemote);
                Log.i(TAG, "clear remote view");
                videoEngine.videoClearView(gwRtcSurfaceVideoRenderRemote);
            });
        }

        @Override
        public void onLocalStreamRemove() {
            runOnUiThread(()->{
                //videoEngine.videoDetachLocalView(gwRtcSurfaceVideoRenderLocal);
                Log.i(TAG, "clear local view");
                videoEngine.videoClearView(gwRtcSurfaceVideoRenderLocal);
            });
        }

        @Override
        public void onVideoData(byte[] bytes, int i, int i1, int i2, int i3) {

        }

        @Override
        public void onHangup(String remoteid) {
            runOnUiThread(()->{
                Toast.makeText(getApplicationContext(), "remote user hangup video:"+remoteid, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onError(int code, String info) {

        }
    };

    private void initData() {
        pttManager = GWPttEngine.INSTANCE(this);
        videoEngine = GWVideoEngine.INSTANCE();
        pttManager.pttInit(pttObserver, videoEventHandler, null);
        pttManager.pttConfigServer(0,"43.250.33.13", 23003);
        pttManager.pttConfigServer(1,"43.250.33.13", 51883);
        pttManager.pttConfigServer(2,"43.250.33.13", 50001);
        pttManager.pttConfigServer(3,"114.116.246.85", 8188);
    }

    private void initView() {
        btnLogin = findViewById(R.id.loginbutton);
        btnQueryGrp = findViewById(R.id.querygroup);
        btnSwitchGrp = findViewById(R.id.switchgroup);
        btnQueryMembers = findViewById(R.id.querymember);
        btnLogout = findViewById(R.id.ulogout);
        btnReqSpk = findViewById(R.id.ureqmic);
        btnRelSpk = findViewById(R.id.udropmic);
        btnTempGrp = findViewById(R.id.utempcall);
        btnStartMsg = findViewById(R.id.ustartmsg);
        btnSendMsg = findViewById(R.id.usendmsg);
        btnGetWeather = findViewById(R.id.ugetweather);
        btnVideo = findViewById(R.id.videodemo);
        btnVideoCall = findViewById(R.id.videocall);
        btnVideoAccept = findViewById(R.id.videoaccept);
        btnVideoHandup = findViewById(R.id.videohangup);

        editAccount = findViewById(R.id.loginUserName);
        editPassword = findViewById(R.id.loginUserPass);

        gwRtcSurfaceVideoRenderLocal = findViewById(R.id.videoviewlocal);
        gwRtcSurfaceVideoRenderRemote = findViewById(R.id.videoviewremote);
    }

    private void initEvent() {
        //步骤 1.登录
        btnLogin.setOnClickListener(v -> {
            String account = editAccount.getText().toString();
            String password = editPassword.getText().toString();
            pttManager.pttLogin(account, password, "12345", "54321");
        });
        //查询群组
        btnQueryGrp.setOnClickListener(v -> {
            pttManager.pttQueryGroup();
        });
        //进入群组
        btnSwitchGrp.setOnClickListener(v -> {
            pttManager.pttJoinGroup(577427);
        });
        //查询成员
        btnQueryMembers.setOnClickListener(v -> {
            pttManager.pttQueryMember(577427);
        });
        //半双工呼叫
        btnReqSpk.setOnClickListener(v -> {
            pttManager.pttSpeak(GWType.GW_SPEAK_TYPE.GW_PTT_SPEAK_START, System.currentTimeMillis());
        });
        //半双工呼叫
        btnRelSpk.setOnClickListener(v -> {
            pttManager.pttSpeak(GWType.GW_SPEAK_TYPE.GW_PTT_SPEAK_END, System.currentTimeMillis());
        });
        //建立临时群组
        btnTempGrp.setOnClickListener(v -> {
            int[] quids = new int[1];
            quids[0] = 5372356;
            pttManager.pttTempGroup(quids, 1);
        });
        //注册离线消息
        btnStartMsg.setOnClickListener(v ->{
            int[] groups = new int[2];
            groups[0] = 65871;
            groups[1] = 65747;
            pttManager.pttRegOfflineMsg(groups, 2);
        });
        //发送消息
        btnSendMsg.setOnClickListener(v ->{
            pttManager.pttSendMsg(5372355, "公网时代_三号",
                    GWType.GW_MSG_RECV_TYPE.GW_PTT_MSG_RECV_TYPE_USER, 5372356,
                    GWType.GW_MSG_TYPE.GW_PTT_MSG_TYPE_TEXT, "test from android", System.currentTimeMillis(), (char)1);
        });
        //获取天气
        btnGetWeather.setOnClickListener(v ->{
            pttManager.pttGetWeather(12345,12345,4,"460", "03");
        });
        //跳转到视频通信
        btnVideo.setOnClickListener(v -> {
//            Intent intent=new Intent(MainActivity.this, VideoActivity.class);
//            MainActivity.this.startActivity(intent);
        });

        //视频拉取
        btnVideoCall.setOnClickListener(v -> {
            videoEngine.videoPull("5372355", "公网时代_三号", "5372356", false, false,
                    GWVideoEngine.GWVideoPriority.GW_VIDEO_PRIORITY_SMOOTH,
                    GWVideoEngine.GWVideoResolution.GW_VIDEO_RESOLUTION_NORMAL);
            videoEngine.videoCall("gwsd03","5372355", "公网时代_三号", "5372356", false,
                    GWVideoEngine.GWVideoResolution.GW_VIDEO_RESOLUTION_NORMAL);
        });
        btnVideoAccept.setOnClickListener(v -> {
            videoEngine.videoAcceptPull("gwsd03", "5372355", "公网时代_三号", 2, false);
            videoEngine.videoAcceptCall("gwsd03", "5372355", "公网时代_三号");
            videoEngine.videoJoinMeeting("gwsd03", "5372355", "公网时代_三号");
        });
        btnVideoHandup.setOnClickListener(v -> {
            videoEngine.videoHangup("5372355", "公网时代_三号", "5372356");
        });

        //离线
        btnLogout.setOnClickListener(v -> {
            pttManager.pttLogout();
        });
    }

    private void startTimer() {
        disposable = Observable.interval(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                        //Log.d(TAG, "send heart");
                        pttManager.pttHeart(100, "4g", System.currentTimeMillis());
                });
    }

    private void stopTimer() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
    }
}