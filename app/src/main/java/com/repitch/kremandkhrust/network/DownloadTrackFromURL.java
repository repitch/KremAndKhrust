package com.repitch.kremandkhrust.network;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.repitch.kremandkhrust.Consts;
import com.repitch.kremandkhrust.Utils;
import com.repitch.kremandkhrust.ui.activities.BaseActivity;
import com.repitch.kremandkhrust.ui.activities.MainActivity;
import com.vk.sdk.api.model.VKApiAudio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by repitch on 05.05.15.
 */
public class DownloadTrackFromURL extends AsyncTask<VKApiAudio, String, String> {
    private static final String TAG_INITIAL_SIGNAL = "initial signal";
    private static final String TAG_PROCESS_SIGNAL = "process singal";
    private static final String TAG_TRACK_EXISTS = "track already exists signal";

    private double fileSizeMB;
    private long fileSize;
    private long downloadedSize;

    BaseActivity mActivity;
    public DownloadTrackFromURL setActivity(BaseActivity activity){
        mActivity = activity;
        return this;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        ((MainActivity) mActivity).signalDownloadStarted();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        ((MainActivity) mActivity).signalDownloadFinished();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        String tag = values[0];
        switch (tag){
            case TAG_INITIAL_SIGNAL:
                ((MainActivity) mActivity).signalShowTrackSize((int)((double)fileSize/Consts.KILOBYTE));
                break;
            case TAG_PROCESS_SIGNAL:
                ((MainActivity) mActivity).signalDownloadUpdate((int)((double)downloadedSize / Consts.KILOBYTE));
                break;
            case TAG_TRACK_EXISTS:
                ((MainActivity) mActivity).signalTrackExists();
                break;
        }
    }

    @Override
    protected String doInBackground(VKApiAudio... params) {
        VKApiAudio track = params[0];
        Log.e("VKAUDIO","doInBackground ");
        int count;
        try {
            URL url = new URL(track.url);
            URLConnection connection = url.openConnection();
            connection.connect();
            // getting file length
            fileSize = connection.getContentLength();
            publishProgress(TAG_INITIAL_SIGNAL);

            // Output stream to write file
            String mp3Name = track.artist+"-"+track.title+".mp3";
            File cacheFolder = new File(Environment.getExternalStorageDirectory() + Consts.CACHE_PATH);
            if (!cacheFolder.exists()){
                cacheFolder.mkdir();
            }
            String outPath = cacheFolder.getAbsolutePath() + Utils.fixFileName(mp3Name);
            File outFile = new File(outPath);
            if (outFile.exists()&&outFile.length()==fileSize){
                Log.e("VKAUDIO","TAG_TRACK_EXISTS ");
                Log.e("VKAUDIO","size: "+outFile.length());
                Log.e("VKAUDIO","expected: "+fileSize);
            } else {
                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                OutputStream output = new FileOutputStream(outPath);

                byte data[] = new byte[1024];

//            long total = 0;
                downloadedSize = 0;

                while ((count = input.read(data)) != -1) {
                    downloadedSize += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
//                publishProgress(""+(int)((total*100)/lenghtOfFile));
                    publishProgress(TAG_PROCESS_SIGNAL);

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
