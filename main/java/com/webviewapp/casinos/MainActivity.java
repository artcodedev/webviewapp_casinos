package com.webviewapp.webviewapp;

import static android.app.PendingIntent.getActivity;

import android.os.Bundle;
import android.view.View;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


public class MainActivity extends Activity {

    private WebView webView;

    private LinearLayout cont_error_connect;

    private GetUrl geturl = null;

    private String url = null;

    private final static int FILECHOOSER_RESULTCODE = 1;

    private ValueCallback<Uri[]> mUploadMessage;

    private static final int STORAGE_PERMISSION_CODE = 123;

    private ProgressBar progressBar;

    // method for mobile.js
    public class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) {
            mContext = c;
        }
        @JavascriptInterface
        public void showToast() {
            requestStoragePermission();
        }
    }

    private boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private class MyWebViewClient extends WebViewClient {
        @TargetApi(Build.VERSION_CODES.N)

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            invalidateOptionsMenu();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            invalidateOptionsMenu();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

            super.onReceivedError(view, request, error);

            if (error.getErrorCode() == WebViewClient.ERROR_HOST_LOOKUP) {

                cont_error_connect = findViewById(R.id.cont_error_connect);

                webView.setVisibility(View.GONE);

                String html = "<html><body></body></html>";

                webView.loadData(html, "text/html", "UTF-8");

                cont_error_connect.setVisibility(View.VISIBLE);

            } else {
                ShowWebView(url);
            }
        }
    }

    public class GetUrl extends AsyncTask<String, Void, String> {

        private MainActivity activity;

        public GetUrl(MainActivity activity) {
            this.activity = activity;
        }

        @SuppressLint("WrongThread")
        @Override
        protected String doInBackground(String... strings) {

            if (activity != null) {
                try {

                    URL url = new URL("https://your_url?android_app=1");

                    StringBuilder builder = new StringBuilder();

                    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                        String str;
                        while ((str = bufferedReader.readLine()) != null) {
                            builder.append(str);
                        }
                    }
                    String jsonStr = builder.toString();

                    Gson gson = new Gson();

                    JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);

                    String urls_mass = jsonObject.getAsJsonArray("urls").toString();
                    urls_mass = urls_mass.replace('"', ' ');
                    urls_mass = urls_mass.replace('[', ' ');
                    urls_mass = urls_mass.replace(']', ' ');
                    String[] n_urls_mass = urls_mass.split(",");


                    activity.url = n_urls_mass[0].trim() + "?android_app=1";

                    System.out.println("CHECK URL => " + activity.url);

                }

                catch (Exception e) {
                    System.out.println("ERROR TREAD");

                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(String res) {

            System.out.println("onPostExecute start");

            if (activity.checkInternetConnection()) {
                activity.ShowWebView(activity.url);
            }
        }

        public void release() {
            activity = null;
        }

    }
    private void openFileExplorer() {

        Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        i.addCategory(Intent.CATEGORY_OPENABLE);

        i.setType("image/*");

        MainActivity.this.startActivityForResult( Intent.createChooser( i, "File Chooser" ), MainActivity.FILECHOOSER_RESULTCODE );
    }

    private void requestStoragePermission() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            openFileExplorer();
            return;
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Требуються права на открытие файлов!", Toast.LENGTH_SHORT);
            toast.show();
        }

       if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {}
    }

    private class MyWebChromeClient extends WebChromeClient {

        Context context;
        public MyWebChromeClient(Context context) {
            super();
            this.context = context;
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            mUploadMessage = filePathCallback;
            return true;
        }

    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    private void ShowWebView(String url) {

        System.out.println("SHOW WEBVIEW  url => " + url);

        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new MyWebViewClient());

        webView.setWebChromeClient(new MyWebChromeClient(this));

        webView.loadUrl(url);

        cont_error_connect.setVisibility(View.GONE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILECHOOSER_RESULTCODE) {

            if (null == mUploadMessage) return;

            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();

            if (result == null) {
                mUploadMessage.onReceiveValue(null);
            } else {
                mUploadMessage.onReceiveValue(new Uri[]{result});
            }

            mUploadMessage = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        progressBar = findViewById(R.id.webProgressBar);

        cont_error_connect = findViewById(R.id.cont_error_connect);

        geturl = new GetUrl(this);

        geturl.execute();
    }
    public void update_app (View view) {if (checkInternetConnection()) {ShowWebView(url);};}

    public void onBackPressed() {

        if(webView.canGoBack()) {webView.goBack();}

        else {super.onBackPressed();}
    }

    protected void onDestroy() {

        super.onDestroy();

        geturl.release();
    }
}