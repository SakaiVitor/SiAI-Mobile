package com.labprog.siai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FaltasActivity extends AppCompatActivity {

    private ApiService apiService;
    private String sessionId;
    private String userId;
    private boolean isAdmin;
    private Spinner spinnerMeals;
    private DecoratedBarcodeView barcodeView;
    private CaptureManager captureManager;
    private DrawerLayout drawerLayout;
    private TextView loadingTextView;
    private NavigationView navigationView;
    private Button buttonScan;
    private boolean isProcessing = false; // Flag para rastrear se há uma resposta pendente
    private String lastScannedCode = ""; // Variável para armazenar o último QR code escaneado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faltas);
        drawerLayout = findViewById(R.id.drawerLayout); // Inicialize o DrawerLayout
        navigationView = findViewById(R.id.navigationView); // Inicialize o NavigationView
        spinnerMeals = findViewById(R.id.spinnerMeals);
        barcodeView = findViewById(R.id.barcodeView);
        loadingTextView = findViewById(R.id.loadingTextView);
        buttonScan = findViewById(R.id.buttonScan);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionId = getIntent().getStringExtra("sessionId");
        userId = getIntent().getStringExtra("userId");

        Toolbar toolbar = findViewById(R.id.toolbar); // Inicializar Toolbar
        setSupportActionBar(toolbar); // Configurar Toolbar como ActionBar

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.itemMenu:
                    intent = new Intent(FaltasActivity.this, MenuActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemPreencher:
                    intent = new Intent(FaltasActivity.this, ArranchamentoActivity.class);
                    intent.putExtra("sessionId", sessionId);  // Passe o sessionId
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemExportar:
                    intent = new Intent(FaltasActivity.this, ExportarActivity.class);
                    intent.putExtra("sessionId", sessionId);  // Passe o sessionId
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemFaltas:
                    intent = new Intent(FaltasActivity.this, FaltasActivity.class);
                    intent.putExtra("sessionId", sessionId);  // Passe o sessionId
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemSair:
                    logout();
                    break;
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Configurar o spinner com as opções de refeição
        String[] mealOptions = {"Café", "Almoço", "Janta", "Ceia"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mealOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Use the default layout for the Spinner item
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.WHITE); // Change text color to white
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                // Use the default layout for the dropdown item
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.WHITE); // Change text color to white
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeals.setAdapter(adapter);


        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
            }
        });

        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
    }

    private void logout() {
        Intent intent = new Intent(FaltasActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkLoginAndAdmin() {
        if (userId == null || userId.isEmpty()) {
            redirectToMenu("Você precisa estar logado para acessar esta funcionalidade.");
            return;
        }

        Call<ResponseBody> call = apiService.isAdmin(userId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        isAdmin = jsonObject.getBoolean("isAdmin");

                        if (!isAdmin) {
                            redirectToMenu("Você precisa ser admin para acessar esta funcionalidade.");

                        } else {
                            setupUI();
                        }
                    } catch (Exception e) {
                        Log.e("FaltasActivity", "Erro ao processar JSON", e);
                        redirectToMenu("Erro ao verificar privilégios de admin.");
                    }
                } else {
                    redirectToMenu("Erro ao verificar privilégios de admin.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("FaltasActivity", "Erro de rede", t);
                redirectToMenu("Erro de rede ao verificar privilégios de admin.");
            }
        });
    }

    private void redirectToMenu(String message) {
        Toast.makeText(FaltasActivity.this, message, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(FaltasActivity.this, MenuActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    private void setupUI() {
        // Configurar o spinner já foi feito no onCreate
    }

    private void startScanning() {
        barcodeView.setVisibility(View.VISIBLE);
        barcodeView.resume(); // Certifique-se de que o escaneamento está ativo
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null) {
                    String scannedCode = result.getText();
                    if (isProcessing) {
                        Toast.makeText(FaltasActivity.this, "Ainda processando o arranchamento anterior...", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int userId = Integer.parseInt(scannedCode);
                        Log.d("QRCode", "QR code lido com sucesso: " + userId);
                        isProcessing = true;
                        lastScannedCode = scannedCode;
                        Toast.makeText(FaltasActivity.this, "Processando arranchamento...", Toast.LENGTH_SHORT).show();
                        barcodeView.pause(); // Pausar o escaneamento durante o processamento
                        sendMealInfo(userId);
                    } catch (NumberFormatException e) {
                        Toast.makeText(FaltasActivity.this, "QR code inválido", Toast.LENGTH_LONG).show();
                        Log.d("QRCode", "QR code inválido: " + scannedCode);
                    }
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Não utilizado neste exemplo
            }
        });
    }

    private void sendMealInfo(int userId) {
        String mealType = spinnerMeals.getSelectedItem().toString().toLowerCase(Locale.ROOT);
        // Remover acentos dos tipos de refeição
        mealType = Normalizer.normalize(mealType, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        String todayDate = getTodayDateInSqlFormat();

        Log.d("sendMealInfo", "Enviando informações: userId=" + userId + ", mealType=" + mealType + ", date=" + todayDate);

        Call<ResponseBody> call = apiService.sendMealInfo(userId, mealType, todayDate);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isProcessing = false;
                lastScannedCode = ""; // Resetar o último código escaneado após o processamento
                barcodeView.resume(); // Retomar o escaneamento

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        String message = jsonObject.optString("message", "Resposta do servidor não contém mensagem");
                        Toast.makeText(FaltasActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.d("sendMealInfo", "Informações enviadas com sucesso: " + response.toString());
                    } catch (Exception e) {
                        Log.e("sendMealInfo", "Erro ao processar JSON", e);
                    }
                } else {
                    try {
                        String jsonResponse = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        String message = jsonObject.optString("message", "Resposta do servidor não contém mensagem");
                        Toast.makeText(FaltasActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.d("sendMealInfo", "Erro ao enviar informações: " + response.toString());
                    } catch (Exception e) {
                        Toast.makeText(FaltasActivity.this, "Erro ao enviar informações", Toast.LENGTH_LONG).show();
                        Log.e("sendMealInfo", "Erro ao processar JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isProcessing = false;
                lastScannedCode = ""; // Resetar o último código escaneado após falha
                barcodeView.resume(); // Retomar o escaneamento

                Toast.makeText(FaltasActivity.this, "Erro de rede ao enviar informações", Toast.LENGTH_LONG).show();
                Log.d("sendMealInfo", "Erro de rede ao enviar informações", t);
            }
        });
    }

    // Método para obter a data de hoje no formato SQL
    private String getTodayDateInSqlFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
        barcodeView.resume(); // Certifique-se de que o escaneamento está ativo
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
        barcodeView.pause(); // Pausar o escaneamento ao pausar a atividade
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureManager.onSaveInstanceState(outState);
    }
}
