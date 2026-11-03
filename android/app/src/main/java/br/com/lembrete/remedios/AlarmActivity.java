package br.com.lembrete.remedios;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tela cheia do alarme: aparece por cima da tela de bloqueio. */
public class AlarmActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Bundle extras = getIntent().getExtras();
        final android.content.Intent in = getIntent();
        String nome = Estado.nz(in.getStringExtra("nome"));
        String dose = Estado.nz(in.getStringExtra("dose"));
        String hora = Estado.nz(in.getStringExtra("hora"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(109, 94, 252));
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        root.addView(texto("💊", 72, Color.WHITE, false), wrap());
        root.addView(texto("HORA DO REMÉDIO", 15, Color.argb(230, 255, 255, 255), true), wrap());
        root.addView(texto(nome, 36, Color.WHITE, true), wrap());
        root.addView(texto((dose.isEmpty() ? "" : dose + " · ") + "Agendado para " + hora,
                16, Color.argb(230, 255, 255, 255), false), wrap());

        LinearLayout botoes = new LinearLayout(this);
        botoes.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);

        Button tomei = botao("✓ Tomei", Color.WHITE, Color.rgb(109, 94, 252));
        tomei.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Acoes.executar(AlarmActivity.this, "tomei", in); finish(); }
        });
        Button soneca = botao("Lembrar em 10 min", Color.argb(60, 255, 255, 255), Color.WHITE);
        soneca.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Acoes.executar(AlarmActivity.this, "soneca", in); finish(); }
        });
        Button pular = botao("Pular esta dose", Color.argb(40, 0, 0, 0), Color.WHITE);
        pular.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Acoes.executar(AlarmActivity.this, "pular", in); finish(); }
        });

        botoes.addView(tomei, lp);
        botoes.addView(soneca, lp);
        botoes.addView(pular, lp);
        root.addView(botoes, wrap());

        setContentView(root);
        Alarme.tocarLoop(this);
    }

    @Override
    public void onBackPressed() {
        // Não fecha sem agir: o alarme continua até Tomei, Soneca ou Pular
    }

    private TextView texto(String t, int sp, int cor, boolean negrito) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(sp);
        tv.setTextColor(cor);
        tv.setGravity(Gravity.CENTER);
        if (negrito) tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(6), 0, dp(6));
        return tv;
    }

    private Button botao(String t, int fundo, int cor) {
        Button bt = new Button(this);
        bt.setText(t);
        bt.setAllCaps(false);
        bt.setTextSize(18);
        bt.setTextColor(cor);
        bt.setBackgroundColor(fundo);
        return bt;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
