package br.com.lembrete.remedios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Disparado pelo sistema na hora de cada remédio (ou de cada soneca). */
public class AlarmReceiver extends BroadcastReceiver {
    static final String ACAO = "br.com.lembrete.remedios.ALARME";

    @Override
    public void onReceive(Context ctx, Intent in) {
        if (!ACAO.equals(in.getAction())) return;

        String chave = in.getStringExtra("chave");
        String dia = in.getStringExtra("dia");
        boolean soneca = in.getBooleanExtra("soneca", false);
        JSONObject estado = Estado.carregar(ctx);

        // Se a dose já foi registrada, não toca
        if (chave != null && dia != null && Estado.registro(estado, dia, chave) == null) {
            if (soneca) {
                removerAdiado(estado, chave);
                Estado.gravarJson(ctx, estado.toString());
            }
            Alarme.exibir(ctx, in);
        }
        Agenda.reagendar(ctx);
    }

    private static void removerAdiado(JSONObject estado, String chave) {
        JSONArray antigos = estado.optJSONArray("adiados");
        if (antigos == null) return;
        JSONArray novos = new JSONArray();
        long agora = System.currentTimeMillis();
        for (int i = 0; i < antigos.length(); i++) {
            JSONObject a = antigos.optJSONObject(i);
            if (a == null) continue;
            boolean vencido = chave.equals(a.optString("chave")) && a.optLong("quando") <= agora + 2000;
            if (!vencido) novos.put(a);
        }
        try {
            estado.put("adiados", novos);
        } catch (JSONException e) {
            // não acontece
        }
    }
}
