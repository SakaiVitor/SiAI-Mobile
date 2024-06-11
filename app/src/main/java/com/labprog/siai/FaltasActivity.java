package com.labprog.siai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private TextView loadingTextView;
    private Button buttonScan;
    private boolean isScanning = false; // Flag para controlar o estado do escaneamento

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faltas);

        spinnerMeals = findViewById(R.id.spinnerMeals);
        barcodeView = findViewById(R.id.barcodeView);
        loadingTextView = findViewById(R.id.loadingTextView);
        buttonScan = findViewById(R.id.buttonScan);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionId = getIntent().getStringExtra("sessionId");
        userId = getIntent().getStringExtra("userId");
        Log.d("FaltasActivity", "Session ID: " + sessionId);

        // Configurar o spinner com as opções de refeição
        String[] mealOptions = {"café", "almoço", "janta", "ceia"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mealOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeals.setAdapter(adapter);

        checkLoginAndAdmin();

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
            }
        });

        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
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
        intent.putExtra("sessionId", sessionId);
        startActivity(intent);
        finish();
    }

    private void setupUI() {
        // Configurar o spinner já foi feito no onCreate
    }

    private void startScanning() {
        if (!isScanning) {
            barcodeView.setVisibility(View.VISIBLE);
            isScanning = true;
            barcodeView.decodeContinuous(new BarcodeCallback() {
                @Override
                public void barcodeResult(BarcodeResult result) {
                    if (result != null) {
                        try {
                            int userId = Integer.parseInt(result.getText());
                            Log.d("QRCode", "QR code lido com sucesso: " + userId);
                            sendMealInfo(userId);
                            barcodeView.pause(); // Pausar o escaneamento após a leitura
                            isScanning = false; // Resetar a flag de escaneamento
                        } catch (NumberFormatException e) {
                            Toast.makeText(FaltasActivity.this, "QR code inválido", Toast.LENGTH_LONG).show();
                            Log.d("QRCode", "QR code inválido: " + result.getText());
                        }
                    }
                }

                @Override
                public void possibleResultPoints(List<ResultPoint> resultPoints) {
                    // Não utilizado neste exemplo
                }
            });
        }
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
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
