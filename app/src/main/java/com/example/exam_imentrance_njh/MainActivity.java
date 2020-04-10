package com.example.exam_imentrance_njh;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Context mContext;
    public String TAG; //logcat 태그
    public String URL_INIT; //비콘정보 갱신을 위한 init주소
    public String URL_REFRESH; //비콘정보 갱신을 위한 refresh주소
    public String URL_DOMAIN; //webview주소
    public String URL_INDEX_FULL; //webview index full 주소
    public String URL_INDEX; //webview index 폴더 주소

    private WebView wv_entrance;
    private String final_url; //최종 webview에 띄울 url
    private BroadcastReceiver message_receiver; // 푸시오는것을 받는 리시버
    private SoundPool sound_pool;  // 푸시알람음
    private int sound_beep_alert; // 비프음
    private int sound_roop; // 알람음 반복회수
    private Handler h_check_service; // NFC리더기와 연결되어있는지 검사하는 서비스
    private TextView tv_nfc_state; // NFC 리더기와 연결상태 띄워주는 텍스트뷰
    private boolean flag_wv_load_finish;  //webview_출결체크 종료 프레그
    public static String nfc_state; // NFC 리더기 상태

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mContext = getBaseContext();
        Log.i(TAG,"");

        init();
        check_token();
        setFunction();
    }

    private void init() {

        //getString
        this.TAG = getString(R.string.TAG);
        this.URL_INIT = getString(R.string.URL_INIT);
        this.URL_REFRESH = getString(R.string.URL_REFRESH);
        this.URL_DOMAIN = getString(R.string.URL_DOMAIN);
        this.URL_INDEX_FULL = getString(R.string.URL_INDEX_FULL);
        this.URL_INDEX = getString(R.string.URL_INDEX);

        wv_entrance = (WebView) findViewById(R.id.wv01);
        tv_nfc_state = findViewById(R.id.tv_nfc_state);

    }

    private void setFunction() {

        final_url = URL_INDEX_FULL + getSwHwInfo();
        flag_wv_load_finish = false;

        //출결 웹뷰 생성
        wv_entrance.setWebViewClient(new WebViewClient());
        //ssl 인증이 없는 경우 해결을 위한 부분
        wv_entrance.setWebChromeClient(new WebChromeClient() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });
        wv_entrance.getSettings().setJavaScriptEnabled(true);
        wv_entrance.getSettings().setTextZoom(100);
        wv_entrance.loadUrl(final_url);
        wv_entrance.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                // do your stuff here
                flag_wv_load_finish = true;
            }
        });
        //푸시받는 리시버 등록
        message_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub // Get extra data included in the Intent
//                Log.i(TAG, "message_receiver/onReceive");
//                receive_fcm(intent, true);
                startSound();
            }
        };

        // NFC리더기 연동상태 확인 핸들러 구현
        h_check_service = new Handler() {
            public void handleMessage(Message msg) {

                if (msg.what == 0) {
                    h_check_service.sendEmptyMessageDelayed(0, 1000);

                    if (flag_wv_load_finish && wv_entrance.getUrl().equals(URL_INDEX)) {
                        tv_nfc_state.setVisibility(View.VISIBLE);
                    } else {
                        tv_nfc_state.setVisibility(View.GONE);
                    }
                }
            }
        };
        h_check_service.sendEmptyMessage(0);

    }



    //토큰 발급
    private void check_token() {
        FirebaseMessaging.getInstance().subscribeToTopic("notice");
        String token = FirebaseInstanceId.getInstance().getToken();
        //Log.i(TAG, "teacher_token:" + token);
        if (token == null) {
            Intent intent02 = new Intent(MainActivity.this, MainActivity.class);
            finish();
            startActivity(intent02);
        }
    }

    // 알람음 띄우기
    private void startSound() {
        //Log.i(TAG, "startSound(String noti_title)/noti_title: " + noti_title);
        if (sound_pool != null) {
            sound_pool.release();
        }
        sound_pool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        sound_beep_alert = sound_pool.load(this, R.raw.alarm_default, 1);
        sound_roop = 0;

        sound_pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int soundId, int status) {
                sound_pool.play(sound_beep_alert, 1f, 1f, 1, sound_roop, 1f);
            }
        });

    }


    // 하드웨어 정보 가져오기
    private String getSwHwInfo() {
        String token;
        String kidsmarker_version;
        String hw_wifi_mac;
        String hw_model;
        String sw_version;
        String sw_os;

        FirebaseMessaging.getInstance().subscribeToTopic("notice");
        token = FirebaseInstanceId.getInstance().getToken();
        //Log.i(TAG, "MainActivity/token:" + token);
        kidsmarker_version = getAppVersionName();
        hw_wifi_mac = getMACAddress("wlan0");
        hw_model = Build.MODEL;
        sw_version = android.os.Build.VERSION.SDK_INT + "";
        sw_os = "android";
        return "?center_token=" + token + "&kidsmarker_version=" + kidsmarker_version + "&hw_wifi_mac=" + hw_wifi_mac + "&hw_model=" + hw_model + "&sw_version=" + sw_version +
                "&sw_os=" + sw_os;

    }

    //앱버전 출력
    public String getAppVersionName() {
        PackageInfo packageInfo = null;         //패키지에 대한 전반적인 정보
        //PackageInfo 초기화
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        return packageInfo.versionName;
    }

    // 맥주소 가져오기
    public String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } // for now eat exceptions
        return "";
    }

    @Override
    protected void onResume() {
        super.onResume();

        //FCM 푸시받는 리시버 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(message_receiver, new IntentFilter("MainActivity"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        //FCM 푸시받는 리시버 해지
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(message_receiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
