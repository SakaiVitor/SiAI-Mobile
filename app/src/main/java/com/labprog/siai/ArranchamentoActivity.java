package com.labprog.siai;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArranchamentoActivity extends AppCompatActivity {

    private ApiService apiService;
    private LinearLayout weeksContainer;
    private String sessionId;
    private View loader;
    private Map<String, Boolean> checkboxStates = new HashMap<>();
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arranchamento);

        weeksContainer = findViewById(R.id.weeksContainer);
        Button enviarButton = findViewById(R.id.enviarButton);
        Button exibirMaisButton = findViewById(R.id.exibirMaisButton);
        loader = findViewById(R.id.loader);

        apiService = ApiClient.getClient().create(ApiService.class);

        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, 2);

        carregarDados();

        enviarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarArranchamento();
            }
        });

        exibirMaisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carregarProximaSemana();
            }
        });
    }

    private void carregarDados() {
        Call<ResponseBody> call = apiService.getArranchamentoData("true", sessionId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONArray jsonArray = new JSONArray(jsonResponse);
                        Map<String, Boolean> arranchadosMap = new HashMap<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String date = obj.getString("data");
                            String meal = obj.getString("refeicao").toLowerCase(Locale.ROOT);
                            String key = date + "_" + meal;
                            arranchadosMap.put(key, true);
                        }
                        renderizarDias(arranchadosMap);
                    } catch (Exception e) {
                        Log.e("ArranchamentoActivity", "Erro ao processar JSON", e);
                    }
                } else {
                    Toast.makeText(ArranchamentoActivity.this, "Erro ao carregar dados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("ArranchamentoActivity", "Erro de rede", t);
                Toast.makeText(ArranchamentoActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderizarDias(Map<String, Boolean> arranchadosMap) {
        String[] meals = {"Café", "Almoço", "Janta", "Ceia"};
        renderizarSemana(arranchadosMap, meals);
    }

    private void renderizarSemana(Map<String, Boolean> arranchadosMap, String[] meals) {
        HorizontalScrollView weekScrollView = new HorizontalScrollView(this);
        weekScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout weekLayout = new LinearLayout(this);
        weekLayout.setOrientation(LinearLayout.HORIZONTAL);
        weekScrollView.addView(weekLayout);

        LinearLayout mealLabelsLayout = new LinearLayout(this);
        mealLabelsLayout.setOrientation(LinearLayout.VERTICAL);
        mealLabelsLayout.setPadding(0, 40, 0, 0); // Ajustar padding para alinhar

        for (String meal : meals) {
            TextView mealLabel = new TextView(this);
            mealLabel.setText(meal);
            mealLabel.setTextSize(18);
            mealLabel.setGravity(View.TEXT_ALIGNMENT_CENTER);
            mealLabelsLayout.addView(mealLabel);
        }

        weekLayout.addView(mealLabelsLayout);

        String[] diasDaSemana = {"Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado"};

        for (int i = 0; i < 7; i++) {
            LinearLayout dayLayout = new LinearLayout(this);
            dayLayout.setOrientation(LinearLayout.VERTICAL);

            String formattedDate = String.format("%02d/%02d/%02d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR) % 100);
            TextView dayText = new TextView(this);
            dayText.setText(String.format("%s\n%s", diasDaSemana[calendar.get(Calendar.DAY_OF_WEEK) - 1], formattedDate));
            dayText.setTextSize(16);

            dayLayout.addView(dayText);

            for (int j = 1; j <= meals.length; j++) {
                CheckBox mealCheckBox = new CheckBox(this);
                mealCheckBox.setTextSize(18);
                String key = formattedDate + "_" + j;
                mealCheckBox.setTag(key);

                if (arranchadosMap.containsKey(key)) {
                    mealCheckBox.setChecked(true);
                }

                dayLayout.addView(mealCheckBox);
            }

            weekLayout.addView(dayLayout);
            calendar.add(Calendar.DAY_OF_WEEK, 1);
        }

        weeksContainer.addView(weekScrollView);
        calendar.add(Calendar.DAY_OF_WEEK, -7); // Voltar 7 dias para garantir que a contagem de semanas continue correta
    }

    private void carregarProximaSemana() {
        saveCheckboxStates();
        calendar.add(Calendar.WEEK_OF_YEAR, 1); // Mover para a próxima semana antes de renderizar
        String[] meals = {"Café", "Almoço", "Janta", "Ceia"};
        renderizarSemana(new HashMap<>(), meals);
    }

    private void saveCheckboxStates() {
        int weekCount = weeksContainer.getChildCount();
        for (int w = 0; w < weekCount; w++) {
            HorizontalScrollView weekScrollView = (HorizontalScrollView) weeksContainer.getChildAt(w);
            LinearLayout weekLayout = (LinearLayout) weekScrollView.getChildAt(0);
            int dayCount = weekLayout.getChildCount();
            for (int d = 1; d < dayCount; d++) { // start from 1 to skip mealLabelsLayout
                LinearLayout dayLayout = (LinearLayout) weekLayout.getChildAt(d);
                int mealCount = dayLayout.getChildCount();
                for (int j = 1; j < mealCount; j++) {
                    CheckBox mealCheckBox = (CheckBox) dayLayout.getChildAt(j);
                    checkboxStates.put((String) mealCheckBox.getTag(), mealCheckBox.isChecked());
                }
            }
        }
    }

    private void restoreCheckboxStates() {
        int weekCount = weeksContainer.getChildCount();
        for (int w = 0; w < weekCount; w++) {
            HorizontalScrollView weekScrollView = (HorizontalScrollView) weeksContainer.getChildAt(w);
            LinearLayout weekLayout = (LinearLayout) weekScrollView.getChildAt(0);
            int dayCount = weekLayout.getChildCount();
            for (int d = 1; d < dayCount; d++) { // start from 1 to skip mealLabelsLayout
                LinearLayout dayLayout = (LinearLayout) weekLayout.getChildAt(d);
                int mealCount = dayLayout.getChildCount();
                for (int j = 1; j < mealCount; j++) {
                    CheckBox mealCheckBox = (CheckBox) dayLayout.getChildAt(j);
                    if (checkboxStates.containsKey(mealCheckBox.getTag())) {
                        mealCheckBox.setChecked(checkboxStates.get(mealCheckBox.getTag()));
                    }
                }
            }
        }
    }

    private void enviarArranchamento() {
        saveCheckboxStates();

        List<String> arranchamentos = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : checkboxStates.entrySet()) {
            if (entry.getValue()) {
                arranchamentos.add(entry.getKey());
            }
        }

        Call<ResponseBody> call = apiService.enviarArranchamento(arranchamentos.toArray(new String[0]), "true");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ArranchamentoActivity.this, "Arranchamento enviado com sucesso", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ArranchamentoActivity.this, "Erro ao enviar arranchamento", Toast.LENGTH_SHORT).show();
                    Log.e("ArranchamentoActivity", "Erro ao enviar arranchamento: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("ArranchamentoActivity", "Erro de rede", t);
                Toast.makeText(ArranchamentoActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
