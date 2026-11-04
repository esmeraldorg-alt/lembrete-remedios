package br.com.lembrete.remedios;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Agenda um alarme do sistema para cada horário de remédio e para cada soneca pendente.
 * Usa setAlarmClock: dispara com o app fechado, em modo soneca e depois de reiniciar.
 */
final class Agenda {
    private static final String PREFS = "agenda";
    private static final String KEY = "agendados";

    private Agenda() {}

    static void reagendar(Context ctx) {
        JSONObject estado = Estado.carregar(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> novas = new HashSet<>();
        long agora = System.currentTimeMillis();

        try {
            JSONArray rems = estado.getJSONArray("remedios");
            for (int i = 0; i < rems.length(); i++) {
                JSONObject r = rems.getJSONObject(i);
                String id = r.getString("id");
                JSONArray horas = r.getJSONArray("horarios");
                String inicio = r.optString("inicio", r.optString("criadoEm", ""));
                for (int j = 0; j < horas.length(); j++) {
                    String hora = horas.getString(j);
                    String chave = Estado.chave(id, hora);
                    Calendar alvo = proximoDisparo(estado, chave, hora, agora, inicio);
                    String key = "d|" + chave;

                    Intent in = intencao(ctx, key);
                    in.putExtra("chave", chave);
                    in.putExtra("remedioId", id);
                    in.putExtra("nome", r.optString("nome"));
                    in.putExtra("dose", r.optString("dose"));
                    in.putExtra("dia", Estado.dia(alvo));
                    in.putExtra("hora", hora);
                    in.putExtra("soneca", false);

                    novas.add(key);
                    agendar(ctx, am, key, alvo.getTimeInMillis(), in);
                }
            }

            JSONArray adiados = estado.getJSONArray("adiados");
            for (int k = 0; k < adiados.length(); k++) {
                JSONObject a = adiados.getJSONObject(k);
                long quando = a.getLong("quando");
                if (quando <= agora) continue;
                String chave = a.getString("chave");
                String key = "s|" + chave + "|" + quando;

                Intent in = intencao(ctx, key);
                in.putExtra("chave", chave);
                in.putExtra("remedioId", a.optString("remedioId"));
                in.putExtra("nome", a.optString("nome"));
                in.putExtra("dose", a.optString("dose"));
                in.putExtra("dia", a.optString("dia"));
                in.putExtra("hora", a.optString("hora"));
                in.putExtra("soneca", true);

                novas.add(key);
                agendar(ctx, am, key, quando, in);
            }
        } catch (JSONException e) {
            // dado incompleto: agenda o que foi possível
        }

        // Cancela alarmes que não existem mais (remédio removido, soneca vencida etc.)
        for (String antiga : prefs.getStringSet(KEY, new HashSet<String>())) {
            if (!novas.contains(antiga)) cancelar(ctx, am, antiga);
        }
        prefs.edit().putStringSet(KEY, novas).apply();
    }

    /** Próximo horário do remédio: hoje, se ainda não passou e não foi registrado; senão amanhã. */
    private static Calendar proximoDisparo(JSONObject estado, String chave, String hora, long agora, String inicio) {
        String[] p = hora.split(":");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(agora);
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
        c.set(Calendar.MINUTE, Integer.parseInt(p[1]));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        // Remédio que começa em data futura: primeiro disparo nessa data
        if (!inicio.isEmpty() && inicio.compareTo(Estado.dia(Calendar.getInstance())) > 0) {
            String[] d = inicio.split("-");
            c.set(Calendar.YEAR, Integer.parseInt(d[0]));
            c.set(Calendar.MONTH, Integer.parseInt(d[1]) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d[2]));
            return c;
        }

        boolean feitoHoje = Estado.registro(estado, Estado.dia(Calendar.getInstance()), chave) != null;
        if (c.getTimeInMillis() <= agora || feitoHoje) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return c;
    }

    static Intent intencao(Context ctx, String key) {
        return new Intent(ctx, AlarmReceiver.class)
                .setAction(AlarmReceiver.ACAO)
                .setData(Uri.parse("lembrete://alarme/" + Uri.encode(key)));
    }

    private static void agendar(Context ctx, AlarmManager am, String key, long quando, Intent in) {
        PendingIntent pi = PendingIntent.getBroadcast(ctx, key.hashCode(), in,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent mostrar = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(quando, mostrar), pi);
    }

    private static void cancelar(Context ctx, AlarmManager am, String key) {
        PendingIntent pi = PendingIntent.getBroadcast(ctx, key.hashCode(), intencao(ctx, key),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}
