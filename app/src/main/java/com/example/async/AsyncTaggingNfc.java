package com.example.async;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.exam_imentrance_njh.R;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


//NFC 리더기로 정상 태깅했을때 로그전송
@RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class AsyncTaggingNfc extends AsyncTask<String, Integer, String> {
    public Context mContext;
    public String URL_DOMAIN ;
    public String TAG ;

    private String response_str = "";


    public AsyncTaggingNfc(Context context) {
        this.mContext = context;

        URL_DOMAIN = mContext.getString(R.string.URL_DOMAIN);
        TAG = mContext.getString(R.string.TAG)+":AsyncInsertNfc";

    }
    @Override
    protected String doInBackground(String... str) {
        String uuid =str[0];
        //json 데이터 담기


        //json 넣기
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("uuid", uuid)
                .build();
        //Log.i(TAG, "AsyncTaggingNfc/uuid:" + uuid);

        //request
        Request request = new Request.Builder()
                .url(URL_DOMAIN +"noti/noti_nfc.php")
                .post(body)
                .build();

        try {
            Response res01 = client.newCall(request).execute();
            response_str = res01.body().string();
            Log.i(TAG, "AsyncTaggingNfc()/response_str:" + response_str);
        } catch (IOException e) {
            Log.e(TAG, "AsyncTaggingNfc()/ e.getMessage: " + e.getMessage());
            e.printStackTrace();
        }



        return response_str;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Toast.makeText(mContext, "통신결과 : " + s, Toast.LENGTH_SHORT).show();

    }
}