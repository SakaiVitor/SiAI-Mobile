package com.labprog.siai;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ExportarActivity extends AppCompatActivity {
    private EditText dataInicioField, dataFinalField, turmaField, pelotaoField;
    private Button exportarButton;

    private ApiService apiService;

    private static final String TAG = "ExportarActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exportar);

        dataInicioField = findViewById(R.id.data_inicio);
        dataFinalField = findViewById(R.id.data_final);
        turmaField = findViewById(R.id.turma);
        pelotaoField = findViewById(R.id.pelotao);
        exportarButton = findViewById(R.id.exportar);
        apiService = ApiClient.getClient().create(ApiService.class);

        exportarButton.setOnClickListener(v -> {
            String dataInicio = dataInicioField.getText().toString();
            String dataFinal = dataFinalField.getText().toString();
            String turma = turmaField.getText().toString();
            String pelotao = pelotaoField.getText().toString();

            if (dataInicio.isEmpty() || dataFinal.isEmpty() || pelotao.isEmpty() || turma.isEmpty()) {
                Toast.makeText(ExportarActivity.this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }
            exportar(dataInicio, dataFinal, turma, pelotao);
        });
        // Configure onClickListeners etc.
    }

    private void exportar(String dataInicio, String dataFinal, String turma, String pelotao) {
        Call<Void> call = apiService.export(dataInicio, dataFinal, turma, pelotao);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    try {
                        //String jsonResponse = response.body();
                        //JSONObject jsonObject = new JSONObject(jsonResponse);

                        Toast.makeText(ExportarActivity.this, "Export bem-sucedido!", Toast.LENGTH_SHORT).show();
                        // Redirecionar para MenuActivity
                        Intent intent = new Intent(ExportarActivity.this, MenuActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Erro ao processar JSON", e);
                        Toast.makeText(ExportarActivity.this, "Erro ao processar dados", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ExportarActivity.this, "Falha ao exportar " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                exportarButton.setEnabled(true);
                Log.e(TAG, "Erro de comunicação: ", t);
                Toast.makeText(ExportarActivity.this, "Erro de comunicação", Toast.LENGTH_SHORT).show();
            }

        });
    }
}

