package com.example.activity;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.exam_imentrance_njh.R;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.net.NetworkInterface;
import java.util.ArrayList;
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
    public static final int REQUEST_ENABLE_BT = 1; //블루투스 실행권한 번호
    public static final int REQUEST_ACCESS_COARSE_LOCATION = 2; // GPS 실행권한 번호

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

    private boolean mScanning; //스캔중인지 FLAG
    private LeDeviceListAdapter mLeDeviceListAdapter; // 스캔된 bt 기기 리스트를 보여주는 어댑터
    private BluetoothAdapter mBluetoothAdapter; // 블루투스 어댑터
    public static final String SERIAL_NUMBER = "012505"; //특정 리더기만 매칭시키고싶을때 S/N끝자리 숫자 5개를 입력하면 됨.
    public static Boolean is_live_readeractivity; // 리더 액티비티가 살아있는지 우뮤

    //flag
    private boolean flag_continue_init; //webview_init 종료 프레그
    private boolean flag_continue_refresh_beacon; //webview_refresh_beacon 종료 프레그
    private boolean flag_continue_check_service; // NFC 리더기 연결체크 서비스 종료 프레그

    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mContext = getBaseContext();
        Log.i(TAG,"");

        init();
        check_token();
        getPermissionBt();
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

        // 모든 핸들러 종료
        flag_continue_init = true;
        flag_continue_refresh_beacon = true;
        flag_continue_check_service = true;

        wv_entrance = (WebView) findViewById(R.id.wv01);
        tv_nfc_state = findViewById(R.id.tv_nfc_state);
        mScanning = false;
        is_live_readeractivity = false;


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
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void handleMessage(Message msg) {

                if (msg.what == 0 && flag_continue_check_service) {
                    h_check_service.sendEmptyMessageDelayed(0, 1000);

                    if (flag_wv_load_finish && wv_entrance.getUrl().equals(URL_INDEX)) {
                        tv_nfc_state.setVisibility(View.VISIBLE);
                    } else {
                        tv_nfc_state.setVisibility(View.GONE);
                    }
                }

                if (!nfc_state.equals("GATT_SUCCESS")) {
                    startScanner();
                    tv_nfc_state.setBackground(getResources().getDrawable(R.drawable.rounded_squre_gray, null));
                    tv_nfc_state.setText(" 리더기 OFF ");
                } else {
                    stopScanner();
                    tv_nfc_state.setBackground(getResources().getDrawable(R.drawable.rounded_squre_blue, null));
                    tv_nfc_state.setText(" 리더기 ON ");
                }
            }
        };
        h_check_service.sendEmptyMessage(0);

        // NFC 리더기 상태 초기화
        nfc_state = " NFC Off ";

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
        // 모든 핸들러 종료
        flag_continue_init = false;
        flag_continue_refresh_beacon = false;
        flag_continue_check_service = false;

        ReaderActivityOriginal.disconnectReaderStatic();
        stopScanner();
    }


    //스캐터 시작
    public void startScanner() {
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        scanLeDevice(true);
    }

    //스캐터 종료
    public void stopScanner() {
        scanLeDevice(false);
        if (mLeDeviceListAdapter != null) mLeDeviceListAdapter.clear();
    }

    //블루투스 스캐너 실행 (true 실행, / false 종료)
    private synchronized void scanLeDevice(final boolean enable) {
        if (enable && !mScanning) { //스캔이 필요할 시
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            Log.i(TAG, "scanLeDevice/enable&&!mScanning/nfc_state:" + nfc_state);
        } else if (enable && mScanning) {// 스캔 중에 스캔해라고 명령이 왔을 시
            Log.i(TAG, "--scanLeDevice/enable&&mScanning/nfc_state:" + nfc_state);

        } else if (!enable && mScanning) {// 스캔중에 정지 명령이 왔을시
            Log.i(TAG, "scanLeDevice/!enable&&mScanning/nfc_state:" + nfc_state);
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        } else if (!enable && !mScanning) { // 스캔하지않을때 스캔중지 명령이 왓을시
            Log.i(TAG, "--scanLeDevice/!enable&&!mScanning/nfc_state:" + nfc_state);
        }
    }

    // bt 리스트 를 관리하는 어댑터
    /* Adapter for holding devices found through scanning. */
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return view;
        }
    }

    //블루투스 퍼미션 요청
    private void getPermissionBt() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "블루투스를 켜주세요.", Toast.LENGTH_SHORT)
                    .show();
            onDestroy();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            /* Request access coarse location permission. */
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
    }

    // 스캔된 bt리스트 결과를 받는 어댑터
    /* Device scan callback. */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            //블루투스기기가 스캔은 됐지만 기기이름이 불분명하지 않은경우만 인식시킴
            if (device.getName() != null) {
                Log.i(TAG, "device.name:" + device.getName());

                //(중요) NFC 리더기ACR1255의 디바이스네임에는 항상 "ACR1255U-J1-" 이게 붙어있음
                // 기기별 구분자로 ACR1255U-J1-뒤에 시리얼넘버 5자리가 붙어있음 ex) ACR1255U-J1-12345
                // 따라서 특정한 리더기만 인식되게 하려면 MainActivity.SERIAL_NUMBER 여기에 리더기 뒤에있는 시리얼넘버를 넣어주면 됨
                if (device.getName().contains("ACR1255U-J1-" + MainActivity.SERIAL_NUMBER) && !MainActivity.is_live_readeractivity) {
                    Log.i(TAG, "(sn)Find!~~~ : " + device.toString());

                    //연결 됨
                    MainActivity.is_live_readeractivity = true;
                    mLeDeviceListAdapter.addDevice(device); // 연결된 디바이스 어댑터에 등록
                    mLeDeviceListAdapter.notifyDataSetChanged(); // 어탭터 갱신

                    // NFC리더기가 연결되었으므로 더이상 bt기기를 찾을 필요가없으니 스캔종료
                    scanLeDevice(false);

                    // NFC리더기 발견했으므로 Reader액티비티로 넘어가기
                    Intent dialogIntent = new Intent(MainActivity.this, ReaderActivityOriginal.class);
                    dialogIntent.putExtra(ReaderActivityOriginal.EXTRAS_DEVICE_NAME, device.getName());
                    dialogIntent.putExtra(ReaderActivityOriginal.EXTRAS_DEVICE_ADDRESS,
                            device.getAddress());
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);
                }
            }
        }
    };
}
