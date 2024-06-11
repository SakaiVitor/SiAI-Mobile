package com.labprog.siai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private SurfaceView surfaceViewCamera;
    private TextView loadingTextView;
    private DecoratedBarcodeView barcodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faltas);

        spinnerMeals = findViewById(R.id.spinnerMeals);
        surfaceViewCamera = findViewById(R.id.surfaceViewCamera);
        loadingTextView = findViewById(R.id.loadingTextView);

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

        // Inicializar o SurfaceView para a câmera
        SurfaceHolder surfaceHolder = surfaceViewCamera.getHolder();
        surfaceHolder.addCallback((SurfaceHolder.Callback) this);

        // Inicializar o BarcodeView para o escaneamento do QR code
        barcodeView = new DecoratedBarcodeView(this);
        barcodeView.setStatusText("Posicione o QR code na área de escaneamento");
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous((BarcodeCallback) this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Inicializar a câmera quando o SurfaceView é criado
        barcodeView.getBarcodeView().startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Lidar com mudanças de superfície, se necessário
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Parar a câmera quando o SurfaceView é destruído
        barcodeView.pause();
    }

    @Override
    public void onScanResult(Result result) {
        // Lidar com o resultado do escaneamento do QR code
        if (result != null) {
            try {
                int userId = Integer.parseInt(result.getText());
                Log.d("QRCode", "QR code lido com sucesso: " + userId);
                sendMealInfo(userId);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "QR code inválido", Toast.LENGTH_LONG).show();
                Log.d("QRCode", "QR code inválido: " + result.getContents());
            }
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
                if (response.isSuccessful()) {
                    Toast.makeText(FaltasActivity.this, "Informações enviadas com sucesso", Toast.LENGTH_LONG).show();
                    Log.d("sendMealInfo", "Informações enviadas com sucesso: " + response.toString());
                } else {
                    Toast.makeText(FaltasActivity.this, "Erro ao enviar informações", Toast.LENGTH_LONG).show();
                    Log.d("sendMealInfo", "Erro ao enviar informações: " + response.toString());
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
}
