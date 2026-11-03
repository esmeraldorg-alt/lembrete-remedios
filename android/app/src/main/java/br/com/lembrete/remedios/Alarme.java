package br.com.lembrete.remedios;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Mostra a notificação de alarme e controla o som e a vibração. */
final class Alarme {
    static final String CANAL = "alarme_remedios";
    static final int NOTIF_ID = 4242;
    private static final long[] PADRAO = {0, 600, 300, 600, 300};

    private static MediaPlayer player;
    private static Vibrator vibrador;

    private Alarme() {}

    static void exibir(Context ctx, Intent in) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        criarCanal(nm);

        String nome = Estado.nz(in.getStringExtra("nome"));
        String dose = Estado.nz(in.getStringExtra("dose"));
        String hora = Estado.nz(in.getStringExtra("hora"));

        Intent tela = new Intent(ctx, AlarmActivity.class)
                .putExtras(in)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent telaPi = PendingIntent.getActivity(ctx, 1, tela,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = new Notification.Builder(ctx, CANAL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Hora do remédio: " + nome)
                .setContentText((dose.isEmpty() ? "" : dose + " · ") + hora)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(telaPi)
                .setFullScreenIntent(telaPi, true)
                .addAction(new Notification.Action.Builder(0, "Tomei", acao(ctx, "tomei", in, 11)).build())
                .addAction(new Notification.Action.Builder(0, "Soneca 10 min", acao(ctx, "soneca", in, 12)).build())
                .addAction(new Notification.Action.Builder(0, "Pular", acao(ctx, "pular", in, 13)).build());

        nm.notify(NOTIF_ID, b.build());
    }

    private static PendingIntent acao(Context ctx, String tipo, Intent in, int requestCode) {
        Intent it = new Intent(ctx, ActionReceiver.class)
                .setAction(ActionReceiver.ACAO)
                .setData(Uri.parse("lembrete://acao/" + tipo + "/" + Uri.encode(Estado.nz(in.getStringExtra("chave")))))
                .putExtras(in)
                .putExtra("acao", tipo);
        return PendingIntent.getBroadcast(ctx, requestCode, it,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void criarCanal(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CANAL, "Alarmes de remédio",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
        ch.enableVibration(true);
        ch.setVibrationPattern(PADRAO);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    /** Toca o alarme em loop até o usuário agir. */
    static void tocarLoop(Context ctx) {
        pararSom();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            player = null;
        }
        vibrador = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrador != null) {
            vibrador.vibrate(VibrationEffect.createWaveform(PADRAO, 0));
        }
    }

    static void pararSom() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException e) {
                // já parado
            }
            player.release();
            player = null;
        }
        if (vibrador != null) {
            vibrador.cancel();
            vibrador = null;
        }
    }

    static void parar(Context ctx) {
        pararSom();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIF_ID);
    }
}
