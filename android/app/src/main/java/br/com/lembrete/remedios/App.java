package br.com.lembrete.remedios;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/** Guarda o erro de qualquer travamento para mostrar na próxima abertura do app. */
public class App extends Application {

    static final String DIAG = "diagnostico";

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler padrao = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    SharedPreferences p = getSharedPreferences(DIAG, Context.MODE_PRIVATE);
                    p.edit().putString("erro", new Date().toString() + "\n\n" + sw.toString()).commit();
                } catch (Throwable ignorado) {
                    // não impede o fechamento normal
                }
                if (padrao != null) padrao.uncaughtException(t, e);
            }
        });
    }
}
