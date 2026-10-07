package com.example.avaliacaomobile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_GOAL = 20000;   // limite de meta (ml)
    private static final int MAX_RECORD = 5000;  // limite por registro (ml)
    private static final int RED = Color.parseColor("#D32F2F");
    private static final int BLACK = Color.parseColor("#212121");

    private WaterStorage storage;
    private WaterProgressView waterView;
    private TextView tvAccumulated, tvAlert;
    private EditText etGoal, etVolume;
    private ArrayAdapter<String> adapter;
    private final List<String> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new WaterStorage(this);

        waterView = findViewById(R.id.waterView);
        tvAccumulated = findViewById(R.id.tvAccumulated);
        tvAlert = findViewById(R.id.tvAlert);
        etGoal = findViewById(R.id.etGoal);
        etVolume = findViewById(R.id.etVolume);
        Button btnGoal = findViewById(R.id.btnGoal);
        Button btnAdd = findViewById(R.id.btnAdd);
        ListView listView = findViewById(R.id.listRecords);

        adapter = new ArrayAdapter<>(this, R.layout.item_record, items);
        listView.setAdapter(adapter);

        btnGoal.setOnClickListener(v -> {
            int goal = readValue(etGoal, MAX_GOAL);
            if (goal < 0) return;
            storage.setGoal(goal); // registros do dia são mantidos
            refresh();
        });

        btnAdd.setOnClickListener(v -> {
            int ml = readValue(etVolume, MAX_RECORD);
            if (ml < 0) return;
            storage.addRecord(ml);
            etVolume.setText("");
            refresh();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ao abrir (ou voltar ao app) verifica se o dia mudou e recarrega os dados salvos
        storage.checkNewDay();
        etGoal.setText(String.valueOf(storage.getGoal()));
        refresh();
    }

    // Lê um número positivo do campo. Retorna -1 (e avisa) se for inválido.
    private int readValue(EditText et, int max) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) {
            toast(getString(R.string.error_empty));
            return -1;
        }
        try {
            int v = Integer.parseInt(s);
            if (v <= 0) {
                toast(getString(R.string.error_not_positive));
                return -1;
            }
            if (v > max) {
                toast(getString(R.string.error_too_large, max));
                return -1;
            }
            return v;
        } catch (NumberFormatException e) {
            toast(getString(R.string.error_invalid));
            return -1;
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // Atualiza toda a tela a partir dos valores PERSISTIDOS
    private void refresh() {
        int goal = storage.getGoal();
        int acc = storage.getAccumulated();

        waterView.setMaxValue(goal);
        waterView.setConsumed(acc);

        // O alerta é calculado com os valores salvos, não com um efeito visual temporário
        boolean exceeded = acc > goal;
        tvAccumulated.setText(getString(R.string.summary_accumulated_goal, acc, goal));
        tvAccumulated.setTextColor(exceeded ? RED : BLACK);
        if (exceeded) {
            tvAlert.setText(getString(R.string.alert_goal_exceeded, acc - goal));
            tvAlert.setVisibility(View.VISIBLE);
        } else {
            tvAlert.setVisibility(View.GONE);
        }

        // Mais recente primeiro, com o acumulado de cada operação
        items.clear();
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        int running = 0;
        for (WaterStorage.Record r : storage.getRecords()) {
            running += r.ml;
            items.add(getString(R.string.record_item, fmt.format(new Date(r.time)), r.ml, running));
        }
        Collections.reverse(items);
        adapter.notifyDataSetChanged();
    }
}