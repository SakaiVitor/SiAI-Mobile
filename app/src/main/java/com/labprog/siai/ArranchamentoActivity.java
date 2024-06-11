package com.labprog.siai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Map<String, Boolean> arranchadosMap = new HashMap<>(); // Alterado aqui

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

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

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            if(item.getItemId()==R.id.itemMenu){
                startActivity(new Intent(ArranchamentoActivity.this, MenuActivity.class));
            } else if(item.getItemId()==R.id.itemPreencher){
                startActivity(new Intent(ArranchamentoActivity.this, ArranchamentoActivity.class));
            } else if (item.getItemId()==R.id.itemExportar) {
                startActivity(new Intent(ArranchamentoActivity.this, ExportarActivity.class));
            }else if(item.getItemId()==R.id.itemSair){
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
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
                        Log.d("ArranchamentoActivity", "JSON Recebido: " + jsonResponse);
                        JSONArray jsonArray = new JSONArray(jsonResponse);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String date = obj.getString("data");
                            String meal = obj.getString("refeicao").toLowerCase(Locale.ROOT);
                            String key = date + "_" + meal;
                            arranchadosMap.put(key, true);
                            Log.d("ArranchamentoActivity", "Adicionado ao mapa: " + key);
                        }
                        renderizarDias();
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

    private void renderizarDias() {
        String[] meals = {"cafe", "almoco", "janta", "ceia"};
        renderizarSemana(meals);
    }


    private void renderizarSemana(String[] meals) {
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
        mealLabelsLayout.setPadding(0, 100, 0, 0);

        for (String meal : meals) {
            TextView mealLabel = new TextView(this);
            mealLabel.setText(meal.substring(0, 1).toUpperCase() + meal.substring(1));
            mealLabel.setTextSize(16);
            mealLabel.setGravity(View.TEXT_ALIGNMENT_CENTER);
            mealLabelsLayout.addView(mealLabel);
            mealLabel.setTextColor(Color.WHITE);
            mealLabel.setPadding(0,25,40,0);
        }

        weekLayout.addView(mealLabelsLayout);

        String[] diasDaSemana = {"Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado"};

        for (int i = 0; i < 7; i++) {
            LinearLayout dayLayout = new LinearLayout(this);
            dayLayout.setOrientation(LinearLayout.VERTICAL);


            String formattedDate = String.format("%02d/%02d/%04d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
            TextView dayText = new TextView(this);
            dayText.setText(String.format("%s\n%s", diasDaSemana[calendar.get(Calendar.DAY_OF_WEEK) - 1], formattedDate));
            dayText.setTextSize(14);
            //dayText.setTextColor(Color.WHITE); // Mudar a cor dos dias da semana para branco
            dayText.setPadding(15, 0, 0, 20);
            dayLayout.addView(dayText);

            for (int j = 0; j < meals.length; j++) {
                CheckBox mealCheckBox = new CheckBox(this);
                mealCheckBox.setTextSize(18);
                String key = formattedDate + "_" + (j + 1); // Usando índice numérico para refeição
                mealCheckBox.setTag(key);

                Log.d("ArranchamentoActivity", "Verificando chave: " + key);
                if (arranchadosMap.containsKey(key)) {
                    mealCheckBox.setChecked(true);
                    Log.d("ArranchamentoActivity", "Checkbox marcada: " + key);
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
        String[] meals = {"cafe", "almoco", "janta", "ceia"};
        renderizarSemana(meals); // Passar o estado atual das checkboxes para renderizar
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
                    String key = (String) mealCheckBox.getTag();
                    boolean isChecked = mealCheckBox.isChecked();
                    checkboxStates.put(key, isChecked);
                    Log.d("ArranchamentoActivity", "Checkbox state saved: " + key + " = " + isChecked);
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

        // Convert dates to the required format and prepare the arranchamento array
        List<String> formattedArranchamentos = new ArrayList<>();
        SimpleDateFormat fromUser = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (String arranchamento : arranchamentos) {
            String[] partes = arranchamento.split("_");
            String date = partes[0];
            String mealType = partes[1];

            // Check if the date is already in the correct format
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                formattedArranchamentos.add(date + "_" + mealType);
            } else {
                try {
                    date = myFormat.format(fromUser.parse(date));
                    formattedArranchamentos.add(date + "_" + mealType);
                } catch (ParseException e) {
                    Log.e("ArranchamentoActivity", "Erro ao converter a data: " + date, e);
                }
            }
        }

        // Convert List to Array
        String[] arranchamentoArray = formattedArranchamentos.toArray(new String[0]);

        // Fetch last displayed date
        String lastDateDisplayed = getLastDateDisplayed();

        // Make API call
        Call<ResponseBody> call = apiService.enviarArranchamento(arranchamentoArray, "true", lastDateDisplayed);
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


    private String getLastDateDisplayed() {
        Calendar lastDateCalendar = Calendar.getInstance();
        lastDateCalendar.setTime(calendar.getTime()); // Certifique-se de que 'calendar' é atualizado corretamente
        return String.format("%02d/%02d/%04d", lastDateCalendar.get(Calendar.DAY_OF_MONTH), lastDateCalendar.get(Calendar.MONTH) + 1, lastDateCalendar.get(Calendar.YEAR));
    }


    private String formatDataForPost() {
        StringBuilder formattedData = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : checkboxStates.entrySet()) {
            if (entry.getValue()) {
                formattedData.append("&arranchamento=").append(entry.getKey());
            }
        }
        // Adiciona o lastDateDisplayed no final
        try {
            formattedData.append("&lastDateDisplayed=").append(URLEncoder.encode(getLastDateDisplayed(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return formattedData.toString();
    }

    private void logout() {
        // Limpar sessão ou qualquer dado de login aqui
        Intent intent = new Intent(ArranchamentoActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
