package br.com.lembrete.remedios;

import android.webkit.JavascriptInterface;

/** Ponte entre o app web (app.js) e o Android: dados e permissões. */
public final class Ponte {
    private final MainActivity activity;

    Ponte(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public String lerEstado() {
        return Estado.nz(Estado.lerJson(activity));
    }

    @JavascriptInterface
    public void gravarEstado(String json) {
        Estado.gravarJson(activity, json);
        Agenda.reagendar(activity);
    }

    @JavascriptInterface
    public void pedirPermissoes() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() { activity.pedirPermissoes(); }
        });
    }
}
