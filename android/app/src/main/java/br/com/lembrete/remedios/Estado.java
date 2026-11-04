package br.com.lembrete.remedios;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/** Guarda os dados do app (mesmo formato usado pelo app.js) no armazenamento do Android. */
final class Estado {
    static final String PREFS = "lembrete";
    static final String CHAVE = "estado";

    private Estado() {}

    static String lerJson(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHAVE, null);
    }

    static void gravarJson(Context c, String json) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CHAVE, json).apply();
    }

    static JSONObject carregar(Context c) {
        try {
            String s = lerJson(c);
            if (s != null && !s.isEmpty()) return new JSONObject(s);
        } catch (JSONException e) {
            // dado inválido: começa vazio, mas registra o erro para o teste
            c.getSharedPreferences("diag", Context.MODE_PRIVATE).edit()
                    .putString("erro_leitura", String.valueOf(e)).apply();
        }
        JSONObject o = new JSONObject();
        try {
            o.put("remedios", new JSONArray());
            o.put("registros", new JSONObject());
            o.put("avisados", new JSONObject());
            o.put("adiados", new JSONArray());
        } catch (JSONException e) {
            // não acontece
        }
        return o;
    }

    static String dia(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static String chave(String remedioId, String hora) {
        return remedioId + "|" + hora;
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }

    static JSONObject registro(JSONObject estado, String dia, String chave) {
        JSONObject regs = estado.optJSONObject("registros");
        if (regs == null) return null;
        JSONObject dd = regs.optJSONObject(dia);
        return dd == null ? null : dd.optJSONObject(chave);
    }

    static void registrar(JSONObject estado, String dia, String chave, String status,
                          String nome, String hora) throws JSONException {
        JSONObject regs = estado.getJSONObject("registros");
        JSONObject dd = regs.optJSONObject(dia);
        if (dd == null) {
            dd = new JSONObject();
            regs.put(dia, dd);
        }
        JSONObject r = new JSONObject();
        r.put("status", status);
        r.put("em", System.currentTimeMillis());
        r.put("nome", nome);
        r.put("hora", hora);
        dd.put(chave, r);
    }

    static void baixarEstoque(JSONObject estado, String remedioId) throws JSONException {
        JSONArray rems = estado.getJSONArray("remedios");
        for (int i = 0; i < rems.length(); i++) {
            JSONObject r = rems.getJSONObject(i);
            if (!remedioId.equals(r.optString("id"))) continue;
            JSONObject e = r.optJSONObject("estoque");
            if (e == null) return;
            int q = e.optInt("quantidade", 0);
            if (q > 0) e.put("quantidade", q - 1);
            return;
        }
    }
}
