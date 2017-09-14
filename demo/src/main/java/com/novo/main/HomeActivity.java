package com.novo.main;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.source.hls.HLSUtils;
import com.google.android.exoplayer2.source.hls.KeyWriter;
import com.google.android.exoplayer2.upstream.novo.TokenManager;
import com.google.gson.Gson;
import com.novo.R;
import com.novo.adapters.VideoAdapter;
import com.novo.models.FileDownloadModel;
import com.novo.models.VideoModel;
import com.novo.network.DownloadTask;
import com.novo.network.EndPoints;
import com.novo.network.ServerHit;
import com.novo.network.ZipHelper;
import com.novo.services.DownloaderService;
import com.novo.util.Utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.novo.util.Utils.TAG;

public class HomeActivity extends AppCompatActivity implements VideoAdapter.ItemListener {

    private Button btnLogin;
    private GridView lvAll;
    private HomeActivity activity;
    private BroadcastReceiver receiver;
    private List<VideoModel> videoModelsList;
    private VideoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        activity = HomeActivity.this;

        initStuff();

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // register a receiver for callbacks
        IntentFilter filter = new IntentFilter("progress_callback");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long startTime = System.currentTimeMillis();
                //do something based on the intent's action
                Bundle b = intent.getExtras();
                FileDownloadModel downloadModel = (FileDownloadModel) b.getSerializable("fileDownloadModelReturned");
                // could be used to update a progress bar or show info somewhere in the Activity
                Log.d(TAG, "onReceive: PROGRESS HOME: " + downloadModel.getProgress() + "%");

                for (int i = 0; i < lvAll.getAdapter().getCount(); i++) {
                    View child = lvAll.getChildAt(i);
                    if (child != null && videoModelsList.get(i).getVideoId().equalsIgnoreCase(downloadModel.getVideoId())) {
                        TextView tvPercentage = (TextView) child.findViewById(R.id.tvPercentage); // todo fix getting a crash here sometimes null pointer
                        ImageView ivDownload = (ImageView) child.findViewById(R.id.ivDownload);

                        switch (downloadModel.getStatus()) {
                            case DOWNLOADING:
                                if (downloadModel.getProgress() == -1) {
                                    tvPercentage.setText(R.string.downloading);
                                } else {
                                    tvPercentage.setText(downloadModel.getProgress() + "%");
                                }
                                break;
                            case DOWNLOADED:
                                tvPercentage.setText(R.string.completed);
                                break;
                            case UNZIPPING:
                                tvPercentage.setText(R.string.processing);
                                break;
                            case UNZIPPED:
                                tvPercentage.setText("");
                                ivDownload.setImageResource(R.drawable.ic_delete_black_24dp);
                                break;
                            case ERROR:
                                tvPercentage.setText(R.string.unable_to_download);
                                break;
                            case CANCELLED:
                                tvPercentage.setText("");
                                break;
                        }
                        break;
                    }
                }
                Log.d(TAG, "onReceive: Time taken to search views in videoModelsList = " + (System.currentTimeMillis() - startTime) + " ms");
            }
        };
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void initStuff() {
        btnLogin = (Button) findViewById(R.id.btnLogin);
        lvAll = (GridView) findViewById(R.id.lvAll);
        ServerHit.JSONTask task = new ServerHit.JSONTask(this, TokenManager.getToken(), "GET", null, null, new ServerHit.ServiceHitResponseListener() {
            @Override
            public void onDone(String response) {
                Log.d(TAG, "onDone: " + response);
                response = "[\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Encrypted Stream - Open Policy\",\n" +
                        "    \"videoId\": \"Gear_640x3642340_750k_open\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Encrypted Stream - Token Auth policy\",\n" +
                        "    \"videoId\": \"Gear_640x3612340_750k_auth\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Gear\",\n" +
                        "    \"videoId\": \"5WT9g212m4outw\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Encrypted Stream - Open Policy\",\n" +
                        "    \"videoId\": \"Gear_640x369870_750k_open\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Encrypted Stream - Token Auth policy\",\n" +
                        "    \"videoId\": \"Gear_640x8360_750k_auth\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Gear\",\n" +
                        "    \"videoId\": \"5WT9gm654outw\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Encrypted Stream - Open Policy\",\n" +
                        "    \"videoId\": \"Gear_54640x360_750k_open\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Encrypted Stream - Token Auth policy\",\n" +
                        "    \"videoId\": \"Gear_63440x360_750k_auth\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"thumbnail\": \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",\n" +
                        "    \"name\": \"Gear\",\n" +
                        "    \"videoId\": \"5WT9gm234outw\"\n" +
                        "  }\n" +
                        "]";
                videoModelsList = getVideoModelsFromResponse(response);
                adapter = new VideoAdapter(activity, R.layout.row_videos_grid, videoModelsList);
                adapter.setItemListener(activity);
                lvAll.setAdapter(adapter);


            }

            @Override
            public void onError(String error) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String response = "[ {  \"thumbnail\" : \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",  \"name\" : \"Encrypted Stream - Open Policy\",  \"videoId\" : \"Gear_640x360_750k_open\"}, {  \"thumbnail\" : \"http://35.154.11.202/VocabimateContentServer/thumbnails/thumbnail.jpg\",  \"name\" : \"Encrypted Stream - Token Auth policy\",  \"videoId\" : \"Gear_640x360_750k_auth\"} ]";
                        onDone(response);
                    }
                });


            }
        });

        String url = EndPoints.getBaseUrl() + "VocabimateContentServer/webapi/video/fetchAll";
        task.execute(url);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(TokenManager.getToken())) { // logging user out
                    LogoutHelper.cleanup(activity);
                    loginButtonTextUpdate();
                    refreshList();
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                Bundle bundle = new Bundle();
                startActivity(intent.putExtras(bundle));
            }
        });

    }

    private void refreshList() {
        adapter.notifyDataSetChanged();
    }

    @NonNull
    private List<VideoModel> getVideoModelsFromResponse(String response) {
        List<VideoModel> items = new ArrayList<>();
        if (TextUtils.isEmpty(response)) {
            return items;
        }
        try {
            JSONArray array = new JSONArray(response);
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                VideoModel model = new Gson().fromJson(jsonObject.toString(), VideoModel.class);
                items.add(model);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return items;
    }

    private void playMediaFromServer(VideoModel model) {
        ServerHit.JSONTask streamTask = new ServerHit.JSONTask(this, TokenManager.getToken(), "GET", null, null, new ServerHit.ServiceHitResponseListener() {
            @Override
            public void onDone(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    String videoUrl = object.getString("videoUrl");
                    // send to player
                    Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                    intent.setData(Uri.parse(videoUrl));
                    intent.setAction(PlayerActivity.ACTION_VIEW);
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {

            }
        });
        streamTask.execute(EndPoints.getBaseUrl() + "VocabimateContentServer/webapi/video/stream?videoId=" + model.getVideoId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loginButtonTextUpdate();
    }

    private void loginButtonTextUpdate() {
        if (!TextUtils.isEmpty(TokenManager.getToken())) {
            btnLogin.setText("Logout");
        } else {
            btnLogin.setText("Login");
        }
    }

    @Override
    public void onVideoPlayClicked(final VideoModel model) {
        File dir = new File(new Utils(activity).getStorageDirectoryExtracts() + model.getVideoId());
        if (Utils.isFolderPresent(dir)) {
            File[] file = dir.listFiles();
            // trying to find my file
            Log.d(TAG, "onVideoPlayClicked: " + ZipHelper.searchFile(file, null));
            ZipHelper.searchFile(file, new ZipHelper.FileListener() {
                @Override
                public void onFileSearchComplete(boolean fileFound, String fileToPlay) {
                    if (fileFound && !TextUtils.isEmpty(fileToPlay)) {
                        Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                        intent.setData(Uri.parse(fileToPlay));
                        intent.setAction(PlayerActivity.ACTION_VIEW);
                        startActivity(intent);
                    } else {
                        Toast.makeText(activity, "Unable to play local video, playing stream.", Toast.LENGTH_SHORT).show();
                        playMediaFromServer(model);
                    }
                }
            });
        } else { // if folder is not present locally, play via server
            playMediaFromServer(model);
        }
    }

    @Override
    public void onDownloadClicked(VideoModel model, final ImageView ivDownload) {
        String serverFileUrl = EndPoints.getBaseUrl() + "VocabimateContentServer/webapi/video/download?videoId=" + model.getVideoId();
        // todo problem with zip file, hardcoded
        final String keyFileUrl = EndPoints.getBaseUrl() + "VocabimateKeyServer/webapi/keys/getKey?videoId=" + model.getVideoId();
        String videoId = HLSUtils.getVideoIdFromUrl(serverFileUrl);
        if(TextUtils.isEmpty(videoId)){
            Toast.makeText(activity, "Video id not found", Toast.LENGTH_SHORT).show();
            return;
        }
        // execute this when the downloader must be fired
        final File sourceZipFile = new File(new Utils(activity).getStorageDirectoryZips() + videoId);
        String fileNameWithOutExt = FilenameUtils.removeExtension(sourceZipFile.getName());
        final File targetDirectory = new File(new Utils(activity).getStorageDirectoryExtracts() + fileNameWithOutExt);
        targetDirectory.mkdir();



        FileDownloadModel fileDownloadModel = new FileDownloadModel()
                .setVideoId(model.getVideoId())
                .setVideoTitle(model.getName())
                .setToken(TokenManager.getToken())
                .setLink("https://drmdemo-94ea7.firebaseapp.com/arc.zip") // todo hard code
//                .setLink(serverFileUrl)
                .setFilePath(sourceZipFile.getAbsolutePath())
                .setTargetDirectoryPath(targetDirectory.getAbsolutePath())
                .setCallBackIntent("progress_callback");

        Intent intent = new Intent(activity, DownloaderService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("fileDownloadModel", fileDownloadModel);
//        bundle.putString("videoId", model.getVideoId());
//        bundle.putString("videoTitle", model.getName());
//        bundle.putString("token", TokenManager.getToken());
//        bundle.putString("link", "https://drmdemo-94ea7.firebaseapp.com/arc.zip");
//        bundle.putString("filePath", sourceZipFile.getAbsolutePath());
//        bundle.putString("targetDirectoryPath", targetDirectory.getAbsolutePath());
//        bundle.putString("CallbackString", "progress_callback");
        startService(intent.putExtras(bundle)); // todo uncomment

//        final DownloadTask downloadTask = new DownloadTask(activity, TokenManager.getToken(), sourceZipFile.getAbsolutePath(), new DownloadTask.DownloadTaskListener() {
//            @Override
//            public void onFileDownload() {
//                new ZipHelper.ZipTask(activity, new ZipHelper.ZipTaskListener() {
//                    @Override
//                    public void onUnzipped(String fileToPlay) {
//                        Log.d(TAG, "onUnzipped: " + fileToPlay);
//                        ivDownload.setImageResource(R.mipmap.ic_download_complete);
//                    }
//                }).execute(sourceZipFile, targetDirectory);
//            }
//        });
//        downloadTask.execute(serverFileUrl);
//
        final File tempKeyFile = new File(new Utils(activity).getTempDirectoryExtracts() + videoId);
        final DownloadTask keyTask = new DownloadTask(activity, "KEY", TokenManager.getToken(), tempKeyFile.getAbsolutePath(), new DownloadTask.DownloadTaskListener() {
            @Override
            public void onFileDownload() {
                KeyWriter writer = new KeyWriter(activity);
                writer.writeByteToFile(writer.readByteToFileUnencryptedData(keyFileUrl, tempKeyFile), keyFileUrl);
                writer.deleteTempKey(tempKeyFile);
            }
        });
        keyTask.execute(keyFileUrl);
    }

    @Override
    public void onDeleteClicked(File directory, ImageView ivDownload) {
        try {
            FileUtils.deleteDirectory(directory);
            ivDownload.setImageResource(R.drawable.ic_file_download_black_24dp);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "onDeleteClicked: unable to delete directory" + e.getLocalizedMessage());
        }
    }


    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent = new Intent(activity, DownloaderService.class);
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
//                case R.id.navigation_notifications:
//                    mTextMessage.setText(R.string.title_notifications);
//                    return true;
                case R.id.navigation_library:
                    mTextMessage.setText(R.string.title_library);
                    stopService(intent);
                    return true;
                case R.id.navigation_others:
                    mTextMessage.setText(R.string.title_others);
                    return true;
            }
            return false;
        }

    };



}
