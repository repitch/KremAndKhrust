package com.repitch.kremandkhrust.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.repitch.kremandkhrust.Consts;
import com.repitch.kremandkhrust.R;
import com.repitch.kremandkhrust.network.DownloadTrackFromURL;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.dialogs.VKCaptchaDialog;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends BaseActivity {

    Button mBtnVKauth;
    ProgressBar mPbTrackDownload, mPbAction;
    TextView mTvTrackDownload;

    private static String sTokenKey = "VK_ACCESS_TOKEN";
    private static String[] sMyScope = new String[]{VKScope.AUDIO, VKScope.FRIENDS, VKScope.PHOTOS, VKScope.NOHTTPS};
    private VKAccessToken vkAccessToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //VKSdk.initialize(sdkListener, Consts.VK_APP_ID, VKAccessToken.tokenFromUrlString(Consts.tokenURL));
        vkAccessToken = VKAccessToken.tokenFromSharedPreferences(this, sTokenKey);
        VKSdk.initialize(sdkListener, Consts.VK_APP_ID, vkAccessToken);

        setContentView(R.layout.activity_main);
        mBtnVKauth = (Button) findViewById(R.id.btnVKauth);
        mBtnVKauth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ЗАГРУЖАЕМ ЮЗЕРОВ
                VKParameters params = new VKParameters();
                params.put("owner_id", "-1959");
                params.put("album_id", "27964367");
                VKRequest request2 = VKApi.audio().get(params);
                //VKRequest request = VKApi.users().get();
                request2.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);

                        VKList<VKApiAudio> tracks = (VKList) response.parsedModel;
                        for (int i=0; i<Math.min(tracks.size(), 10); i++){
                            Log.e("VKAPI","track "+i+": "+tracks.get(i).title);
                        }
                        // засейвим одну песню в /vkcash
                        VKApiAudio track0 = tracks.get(0);
                        // async task
                        new DownloadTrackFromURL().setActivity(MainActivity.this).execute(track0);
                    }

                    @Override
                    public void onError(VKError error) {
                        super.onError(error);
                        Log.e("VKAPI","ERROR");
                    }

                    @Override
                    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                        super.attemptFailed(request, attemptNumber, totalAttempts);
                    }
                });
            }
        });
        mPbTrackDownload = (ProgressBar) findViewById(R.id.pbTrackDownload);
        mPbAction = (ProgressBar) findViewById(R.id.pbAction);
        mTvTrackDownload = (TextView) findViewById(R.id.tvTrackDownload);
    }

    public void signalDownloadStarted(){
        mTvTrackDownload.setText(getString(R.string.download_started));
        mPbAction.setVisibility(View.VISIBLE);
        mPbTrackDownload.setProgress(0);
    }

    public void signalShowTrackSize(int fileSizeKB) {
        double fileSizeMB = ((double)fileSizeKB/Consts.KILOBYTE);
        fileSizeMB = new BigDecimal(fileSizeMB).setScale(2, RoundingMode.HALF_UP).doubleValue();

        Log.e("VKAUDIO","signalShowTrackSize");
        String str = getString(R.string.download_started);
        str+= " Размер файла: "+fileSizeMB;
        mTvTrackDownload.setText(str);
        mPbTrackDownload.setMax(fileSizeKB);
    }

    public void signalDownloadUpdate(int sizeKB) {
        mPbTrackDownload.setProgress(sizeKB);
    }

    public void signalDownloadFinished(){
        mPbAction.setVisibility(View.INVISIBLE);
        mTvTrackDownload.setText(getString(R.string.download_finished));
        mPbTrackDownload.setProgress(mPbTrackDownload.getMax());
    }


    public void signalTrackExists() {
        mPbAction.setVisibility(View.INVISIBLE);
        mTvTrackDownload.setText(getString(R.string.track_exists));
        mPbTrackDownload.setProgress(mPbTrackDownload.getMax());
    }


    /*private void downloadTrack(VKApiAudio track) {
        File folder = new File(Environment.getExternalStorageDirectory() + "/vkcash/");
        if (!folder.exists()) {
            folder.mkdir();
        }

        String destinationPath;
        URL sourceURL;
        InputStream is;
        OutputStream os;
        try {
            sourceURL = new URL(track.url);
            destinationPath = folder.getPath() + track.artist + "-" + track.title + ".mp3";
            is = sourceURL.openStream();
            os = new FileOutputStream(destinationPath);


        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Log.e("VKAUDIO",destinationPath);
            File destination = new File(destinationPath);
            if (!destination.exists()) {
                FileUtils.copyURLToFile(new URL(track.url),destination);
            }
        } catch (FileNotFoundException e) {
            System.out.print("ERROR "+mp3Path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/
    private VKSdkListener sdkListener = new VKSdkListener() {
        @Override
        public void onCaptchaError(VKError captchaError) {
            new VKCaptchaDialog(captchaError).show();
        }

        @Override
        public void onTokenExpired(VKAccessToken expiredToken) {
            VKSdk.authorize(sMyScope);
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(authorizationError.errorMessage)
                    .show();
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
            newToken.saveTokenToSharedPreferences(MainActivity.this, sTokenKey);
            Log.e("VKAPI","new! access token :"+newToken.accessToken);
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
            Log.e("VKAPI","accept! access token :"+token.accessToken);
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VKUIHelper.onResume(this);
        // проверка на существование аксес токена
        vkAccessToken = VKAccessToken.tokenFromSharedPreferences(this, sTokenKey);
        if (vkAccessToken==null){
            VKSdk.authorize(sMyScope);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VKUIHelper.onDestroy(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
    }
}
