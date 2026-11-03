package br.com.lembrete.remedios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Botões da notificação (Tomei, Soneca, Pular). */
public class ActionReceiver extends BroadcastReceiver {
    static final String ACAO = "br.com.lembrete.remedios.ACAO";

    @Override
    public void onReceive(Context ctx, Intent in) {
        Acoes.executar(ctx, in.getStringExtra("acao"), in);
    }
}
