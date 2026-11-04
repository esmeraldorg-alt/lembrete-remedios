package br.com.lembrete.remedios;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarErroAnterior();

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Ponte(this), "AndroidBridge");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.loadUrl("file:///android_asset/www/index.html");

        setContentView(web);
        pedirPermissoes();
        try {
            Agenda.reagendar(this);
        } catch (Throwable t) {
            android.util.Log.e("LembreteRemedios", "Falha ao agendar alarmes", t);
        }
    }

    /** Se o app travou antes, mostra o erro completo para copiar. */
    private void mostrarErroAnterior() {
        android.content.SharedPreferences d = getSharedPreferences(App.DIAG, MODE_PRIVATE);
        String erro = d.getString("erro", null);
        if (erro == null) return;
        d.edit().remove("erro").commit();

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(erro);
        tv.setTextIsSelectable(true);
        tv.setTextSize(12);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        scroll.addView(tv);
        new android.app.AlertDialog.Builder(this)
                .setTitle("O app travou na última vez")
                .setView(scroll)
                .setPositiveButton("OK", null)
                .show();
    }

    void pedirPermissoes() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
