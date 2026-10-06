package com.example.avaliacaomobile;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WaterStorage {

    public static class Record {
        public final int ml;      // volume consumido
        public final long time;   // instante do registro (millis)

        public Record(int ml, long time) {
            this.ml = ml;
            this.time = time;
        }
    }

    public static final int DEFAULT_GOAL = 2000;

    private static final String PREFS = "water_prefs";
    private static final String KEY_GOAL = "goal";
    private static final String KEY_DATE = "date";
    private static final String KEY_ACC = "accumulated";
    private static final String KEY_RECORDS = "records";

    private final SharedPreferences prefs;

    public WaterStorage(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    // Se o dia mudou, descarta registros e acumulado (novo ciclo). A meta é preservada.
    public void checkNewDay() {
        if (!today().equals(prefs.getString(KEY_DATE, ""))) {
            prefs.edit()
                    .putString(KEY_DATE, today())
                    .putInt(KEY_ACC, 0)
                    .putString(KEY_RECORDS, "[]")
                    .apply();
        }
    }

    public int getGoal() {
        return prefs.getInt(KEY_GOAL, DEFAULT_GOAL);
    }

    // Altera só a meta: os registros do dia não são tocados
    public void setGoal(int goal) {
        prefs.edit().putInt(KEY_GOAL, goal).apply();
    }

    public int getAccumulated() {
        return prefs.getInt(KEY_ACC, 0);
    }

    public List<Record> getRecords() {
        List<Record> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_RECORDS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Record(o.getInt("ml"), o.getLong("time")));
            }
        } catch (JSONException e) {
            // dado corrompido: devolve lista vazia
        }
        return list;
    }

    public void addRecord(int ml) {
        checkNewDay(); // garante que o registro entra no dia atual
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_RECORDS, "[]"));
            JSONObject o = new JSONObject();
            o.put("ml", ml);
            o.put("time", System.currentTimeMillis());
            arr.put(o);
            // Registro e acumulado são gravados juntos, na mesma edição
            prefs.edit()
                    .putString(KEY_RECORDS, arr.toString())
                    .putInt(KEY_ACC, getAccumulated() + ml)
                    .apply();
        } catch (JSONException e) {
            // não deve acontecer
        }
    }
}