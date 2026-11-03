package br.com.lembrete.remedios;

import android.util.Log;
import android.webkit.JavascriptInterface;

/** Ponte entre o app web (app.js) e o Android: dados e permissões. Nunca lança erro para o JavaScript. */
public final class Ponte {
    private static final String TAG = "LembreteRemedios";
    private final MainActivity activity;

    Ponte(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public String lerEstado() {
        try {
            return Estado.nz(Estado.lerJson(activity));
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao ler dados", t);
            return "";
        }
    }

    @JavascriptInterface
    public boolean gravarEstado(String json) {
        try {
            Estado.gravarJson(activity, json);
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao gravar dados", t);
            return false;
        }
        try {
            Agenda.reagendar(activity);
        } catch (Throwable t) {
            Log.e(TAG, "Dados salvos, mas falhou o agendamento dos alarmes", t);
        }
        return true;
    }

    @JavascriptInterface
    public void pedirPermissoes() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() { activity.pedirPermissoes(); }
        });
    }
}
