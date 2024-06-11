package com.labprog.siai;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ExportarActivity extends AppCompatActivity {
    private EditText dataInicioField, dataFinalField, turmaField, pelotaoField;
    private Button exportarButton;

    private ApiService apiService;

    private static final String TAG = "ExportarActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private Calendar calendar;


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
        calendar = Calendar.getInstance();

        dataInicioField.setOnClickListener(v -> showDatePickerDialog(dataInicioField));
        dataFinalField.setOnClickListener(v -> showDatePickerDialog(dataFinalField));
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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            if(item.getItemId()==R.id.itemMenu){
                startActivity(new Intent(ExportarActivity.this, MenuActivity.class));
            } else if(item.getItemId()==R.id.itemPreencher){
                startActivity(new Intent(ExportarActivity.this, ArranchamentoActivity.class));
            } else if (item.getItemId()==R.id.itemExportar) {
                startActivity(new Intent(ExportarActivity.this, ExportarActivity.class));
            }else if(item.getItemId()==R.id.itemSair){
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        // Configure onClickListeners etc.
    }
    private void showDatePickerDialog(EditText editText) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                ExportarActivity.this,
                (view, year1, month1, dayOfMonth) -> {
                    calendar.set(year1, month1, dayOfMonth);
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    editText.setText(format.format(calendar.getTime()));
                },
                year, month, day
        );

        datePickerDialog.show();
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
    private void logout() {
        // Limpar sessão ou qualquer dado de login aqui
        Intent intent = new Intent(ExportarActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}

