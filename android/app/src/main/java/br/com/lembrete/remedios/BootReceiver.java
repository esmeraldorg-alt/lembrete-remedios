package br.com.lembrete.remedios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Depois de reiniciar o celular, os alarmes somem: agenda de novo. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        Agenda.reagendar(ctx);
    }
}
