package br.com.lembrete.remedios;

import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Executa Tomei / Pular / Soneca, vindo da notificação ou da tela de alarme. */
final class Acoes {
    private static final long SONECA_MS = 10 * 60 * 1000L;

    private Acoes() {}

    static void executar(Context ctx, String acao, Intent in) {
        if (in.getBooleanExtra("teste", false)) {
            Alarme.parar(ctx);
            return;
        }
        JSONObject estado = Estado.carregar(ctx);
        String chave = Estado.nz(in.getStringExtra("chave"));
        String remedioId = Estado.nz(in.getStringExtra("remedioId"));
        String nome = Estado.nz(in.getStringExtra("nome"));
        String dose = Estado.nz(in.getStringExtra("dose"));
        String dia = Estado.nz(in.getStringExtra("dia"));
        String hora = Estado.nz(in.getStringExtra("hora"));

        try {
            if ("soneca".equals(acao)) {
                JSONArray adiados = estado.optJSONArray("adiados");
                if (adiados == null) adiados = new JSONArray();
                JSONObject a = new JSONObject();
                a.put("chave", chave);
                a.put("remedioId", remedioId);
                a.put("nome", nome);
                a.put("dose", dose);
                a.put("dia", dia);
                a.put("hora", hora);
                a.put("quando", System.currentTimeMillis() + SONECA_MS);
                adiados.put(a);
                estado.put("adiados", adiados);
            } else if ("tomei".equals(acao) || "pular".equals(acao)) {
                JSONObject antes = Estado.registro(estado, dia, chave);
                boolean jaTomado = antes != null && "tomado".equals(antes.optString("status"));
                if ("tomei".equals(acao)) {
                    Estado.registrar(estado, dia, chave, "tomado", nome, hora);
                    if (!jaTomado) Estado.baixarEstoque(estado, remedioId);
                } else {
                    Estado.registrar(estado, dia, chave, "pulado", nome, hora);
                }
            }
            Estado.gravarJson(ctx, estado.toString());
        } catch (JSONException e) {
            // dado inválido: não altera nada
        }

        Alarme.parar(ctx);
        Agenda.reagendar(ctx);
    }
}
