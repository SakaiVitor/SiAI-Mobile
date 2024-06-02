package com.labprog.siai;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArranchamentoActivity extends AppCompatActivity {

    private ApiService apiService;
    private LinearLayout daysContainer;
    private String sessionId;
    private View loader;
    private JSONArray arranchamentoData; // Variável para armazenar os dados de arranchamento

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arranchamento);

        daysContainer = findViewById(R.id.daysContainer);
        Button enviarButton = findViewById(R.id.enviarButton);
        Button loadMoreButton = findViewById(R.id.exibirMaisButton); // Botão para carregar mais semanas
        loader = findViewById(R.id.loader);

        apiService = ApiClient.getClient().create(ApiService.class);

        sessionId = getIntent().getStringExtra("sessionId");

        carregarDados();

        enviarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarArranchamento();
            }
        });

        loadMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadMoreWeeks();
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
                        Log.d("ArranchamentoActivity", "JSON Response: " + jsonResponse);
                        arranchamentoData = new JSONArray(jsonResponse); // Armazena os dados recebidos
                        renderizarDias(arranchamentoData);
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

    private void renderizarDias(JSONArray jsonArray) {
        String[] meals = {"cafe", "almoco", "janta", "ceia"};
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, 0); // Começa na semana atual

        try {
            Log.d("ArranchamentoActivity", "Renderizando dias...");

            for (int i = 0; i < 7; i++) {
                LinearLayout dayLayout = new LinearLayout(this);
                dayLayout.setOrientation(LinearLayout.VERTICAL);

                String formattedDate = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
                TextView dayText = new TextView(this);
                dayText.setText(String.format("%s (%s)", calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, getResources().getConfiguration().locale), formattedDate));
                dayText.setTextSize(16);

                dayLayout.addView(dayText);

                for (int j = 0; j < meals.length; j++) {
                    CheckBox mealCheckBox = new CheckBox(this);
                    mealCheckBox.setText(meals[j]);
                    String key = formattedDate + "_" + (j + 1);
                    mealCheckBox.setTag(key); // Define a tag com o valor da refeição e data

                    for (int k = 0; k < jsonArray.length(); k++) {
                        try {
                            JSONObject obj = jsonArray.getJSONObject(k);
                            String data = obj.getString("data");
                            String refeicao = obj.getString("refeicao");
                            Log.d("ArranchamentoActivity", "Verificando data: " + data + " e refeição: " + refeicao);

                            // Comparando data e refeição
                            if (data.equals(formattedDate) && refeicao.equalsIgnoreCase(meals[j])) {
                                mealCheckBox.setChecked(true);
                                Log.d("ArranchamentoActivity", "Checkbox marcado: " + key);
                                break;
                            }
                        } catch (Exception e) {
                            Log.e("ArranchamentoActivity", "Erro ao verificar refeição", e);
                        }
                    }

                    dayLayout.addView(mealCheckBox);
                }

                daysContainer.addView(dayLayout);
                calendar.add(Calendar.DAY_OF_WEEK, 1);
            }
        } catch (Exception e) {
            Log.e("ArranchamentoActivity", "Erro ao renderizar dias", e);
        }
    }

    private void loadMoreWeeks() {
        if (arranchamentoData == null) {
            Log.e("ArranchamentoActivity", "arranchamentoData é nulo.");
            return;
        }
        String[] meals = {"cafe", "almoco", "janta", "ceia"};
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, 1); // Adiciona uma semana para carregar mais dados

        try {
            Log.d("ArranchamentoActivity", "Carregando mais semanas...");

            for (int i = 0; i < 7; i++) {
                LinearLayout dayLayout = new LinearLayout(this);
                dayLayout.setOrientation(LinearLayout.VERTICAL);

                String formattedDate = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
                TextView dayText = new TextView(this);
                dayText.setText(String.format("%s (%s)", calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, getResources().getConfiguration().locale), formattedDate));
                dayText.setTextSize(16);

                dayLayout.addView(dayText);

                for (int j = 0; j < 4; j++) {
                    CheckBox mealCheckBox = new CheckBox(this);
                    mealCheckBox.setText(meals[j]);
                    String key = formattedDate + "_" + (j + 1);
                    mealCheckBox.setTag(key); // Define a tag com o valor da refeição e data

                    for (int k = 0; k < arranchamentoData.length(); k++) {
                        try {
                            JSONObject obj = arranchamentoData.getJSONObject(k);
                            String data = obj.getString("data");
                            String refeicao = obj.getString("refeicao");
                            Log.d("ArranchamentoActivity", "Verificando data: " + data + " e refeição: " + refeicao);

                            // Comparando data e refeição
                            if (data.equals(formattedDate) && refeicao.equalsIgnoreCase(meals[j])) {
                                mealCheckBox.setChecked(true);
                                Log.d("ArranchamentoActivity", "Checkbox marcado: " + key);
                                break;
                            }
                        } catch (Exception e) {
                            Log.e("ArranchamentoActivity", "Erro ao verificar refeição", e);
                        }
                    }

                    dayLayout.addView(mealCheckBox);
                }

                daysContainer.addView(dayLayout);
                calendar.add(Calendar.DAY_OF_WEEK, 1);
            }
        } catch (Exception e) {
            Log.e("ArranchamentoActivity", "Erro ao carregar mais semanas", e);
        }
    }

    private void enviarArranchamento() {
        int childCount = daysContainer.getChildCount();
        List<String> arranchamentos = new ArrayList<>();

        for (int i = 0; i < childCount; i++) {
            LinearLayout dayLayout = (LinearLayout) daysContainer.getChildAt(i);
            int mealCount = dayLayout.getChildCount();

            for (int j = 1; j < mealCount; j++) { // Ignorando o primeiro elemento que é o TextView
                CheckBox mealCheckBox = (CheckBox) dayLayout.getChildAt(j);
                if (mealCheckBox.isChecked()) {
                    String[] parts = mealCheckBox.getTag().toString().split("_");
                    String formattedDate = parts[0];
                    int mealIndex = Integer.parseInt(parts[1]);
                    arranchamentos.add(formattedDate + "_" + mealIndex);
                }
            }
        }

        Log.d("ArranchamentoActivity", "Arranchamentos para envio: " + arranchamentos.toString());

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
