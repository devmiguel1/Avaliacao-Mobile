package com.example.waterapp;

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

/**
 * Persistência dos dados com SharedPreferences.
 *
 * Por que SharedPreferences? Os dados são poucos e simples (uma meta, um acumulado,
 * uma data e uma lista curta de registros do dia). Não precisamos de consultas nem de
 * relacionamentos, então SQLite/Room seria mais código sem ganho real. Os registros
 * são guardados como texto JSON (org.json já faz parte do Android, sem dependências).
 *
 * Chaves guardadas:
 *   goal        -> meta diária em ml
 *   date        -> dia (yyyy-MM-dd) a que os registros pertencem
 *   accumulated -> consumo acumulado do dia em ml
 *   records     -> lista JSON de {ml, time}
 */
public class WaterStorage {

    /** Um registro individual de consumo. */
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

    /**
     * Compara a data salva com a data do sistema. Se mudou o dia, descarta os registros
     * e o acumulado do dia anterior (novo ciclo). A meta é preservada.
     */
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

    /** Altera a meta sem mexer nos registros do dia. */
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

    /** Adiciona um registro e atualiza o acumulado, tudo na mesma gravação. */
    public void addRecord(int ml) {
        checkNewDay(); // garante que o registro entra no dia atual
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_RECORDS, "[]"));
            JSONObject o = new JSONObject();
            o.put("ml", ml);
            o.put("time", System.currentTimeMillis());
            arr.put(o);
            prefs.edit()
                    .putString(KEY_RECORDS, arr.toString())
                    .putInt(KEY_ACC, getAccumulated() + ml)
                    .apply();
        } catch (JSONException e) {
            // não deve acontecer
        }
    }
}
