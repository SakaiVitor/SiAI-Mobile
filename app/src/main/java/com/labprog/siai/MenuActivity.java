package com.labprog.siai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends AppCompatActivity {

    private ApiService apiService;
    private TableLayout tableLayout;
    private TextView loadingTextView;
    private String sessionId;
    private Button logoutButton, preencherButton, exportarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        tableLayout = findViewById(R.id.tableLayout);
        loadingTextView = findViewById(R.id.loadingTextView);
        logoutButton = findViewById(R.id.logoutButton);
        preencherButton = findViewById(R.id.preencherButton);
        exportarButton = findViewById(R.id.exportarButton);
        apiService = ApiClient.getClient().create(ApiService.class);

        sessionId = getIntent().getStringExtra("sessionId");

        logoutButton.setOnClickListener(v -> logout());
        preencherButton.setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, ArranchamentoActivity.class)));
        exportarButton.setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, ExportarActivity.class)));

        getMenuData();
    }

    private void getMenuData() {
        Call<ResponseBody> call = apiService.getMenuData("true", sessionId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                loadingTextView.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        Log.d("MenuActivity", "JSON Response: " + jsonResponse);
                        if (jsonResponse.startsWith("{")) {
                            JSONObject jsonObject = new JSONObject(jsonResponse);
                            if (jsonObject.has("topUsuariosNomes") && jsonObject.has("topUsuariosOcorrencias")) {
                                JSONArray topUsuariosNomes = jsonObject.getJSONArray("topUsuariosNomes");
                                JSONArray topUsuariosOcorrencias = jsonObject.getJSONArray("topUsuariosOcorrencias");

                                for (int i = 0; i < topUsuariosNomes.length(); i++) {
                                    TableRow tableRow = new TableRow(MenuActivity.this);

                                    TextView nomeTextView = new TextView(MenuActivity.this);
                                    nomeTextView.setText(topUsuariosNomes.getString(i));
                                    nomeTextView.setTextColor(getResources().getColor(android.R.color.white));
                                    nomeTextView.setPadding(8, 8, 8, 8);
                                    tableRow.addView(nomeTextView);

                                    TextView ocorrenciasTextView = new TextView(MenuActivity.this);
                                    ocorrenciasTextView.setText(String.valueOf(topUsuariosOcorrencias.getInt(i)));
                                    ocorrenciasTextView.setTextColor(getResources().getColor(android.R.color.white));
                                    ocorrenciasTextView.setPadding(8, 8, 8, 8);
                                    tableRow.addView(ocorrenciasTextView);

                                    tableLayout.addView(tableRow);
                                }
                            } else {
                                Log.e("MenuActivity", "JSON inesperado: " + jsonResponse);
                                Toast.makeText(MenuActivity.this, "Erro ao processar dados: JSON inesperado", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Trata a resposta HTML como erro
                            Log.e("MenuActivity", "Resposta inesperada do servidor: " + jsonResponse);
                            Toast.makeText(MenuActivity.this, "Erro ao processar dados: Resposta inesperada do servidor", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("MenuActivity", "Erro ao processar JSON", e);
                        Toast.makeText(MenuActivity.this, "Erro ao processar dados", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MenuActivity.this, "Erro ao obter dados: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingTextView.setVisibility(View.GONE);
                Log.e("MenuActivity", "Erro de rede", t);
                Toast.makeText(MenuActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        // Limpar sessão ou qualquer dado de login aqui
        Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
