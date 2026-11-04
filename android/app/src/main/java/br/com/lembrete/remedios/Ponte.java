package br.com.lembrete.remedios;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

    /** Dispara um alarme de teste agora e devolve o estado das notificações. */
    @JavascriptInterface
    public String testar() {
        try {
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            boolean ativas = nm.areNotificationsEnabled();
            boolean perm = Build.VERSION.SDK_INT < 33
                    || activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            if (!perm) {
                activity.runOnUiThread(new Runnable() {
                    @Override public void run() { activity.pedirPermissoes(); }
                });
            }
            Date agora = new Date();
            Intent in = new Intent();
            in.putExtra("nome", "Teste de alarme");
            in.putExtra("dose", "Se você está vendo isto, o alarme funciona");
            in.putExtra("hora", new SimpleDateFormat("HH:mm", Locale.US).format(agora));
            in.putExtra("chave", "teste");
            in.putExtra("remedioId", "");
            in.putExtra("dia", Estado.dia(java.util.Calendar.getInstance()));
            in.putExtra("teste", true);
            Alarme.exibir(activity, in);
            return "Notificações ativas: " + (ativas ? "SIM" : "NÃO")
                    + "\nPermissão de notificação: " + (perm ? "SIM" : "NÃO")
                    + "\n\nPróximos alarmes agendados:\n" + Agenda.resumo(activity)
                    + "\nUm alarme de teste foi disparado agora.";
        } catch (Throwable t) {
            return "Erro no teste: " + t;
        }
    }

    @JavascriptInterface
    public void pedirPermissoes() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() { activity.pedirPermissoes(); }
        });
    }
}
