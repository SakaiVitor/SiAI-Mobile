package com.labprog.siai;

import static android.app.ProgressDialog.show;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {

    private ApiService apiService;
    private TextView textViewRefeicoes;
    private ProgressBar loadingProgressBar;
    private ImageView imageViewQR;
    private String sessionId;
    private String userId;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView textViewFaltas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        textViewFaltas = findViewById(R.id.textViewFaltas);
        textViewRefeicoes = findViewById(R.id.textViewRefeicoes);
        imageViewQR = findViewById(R.id.imageViewQR);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionId = getIntent().getStringExtra("sessionId");
        userId = getIntent().getStringExtra("userId");

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.itemMenu:
                    intent = new Intent(MenuActivity.this, MenuActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemPreencher:
                    intent = new Intent(MenuActivity.this, ArranchamentoActivity.class);
                    intent.putExtra("sessionId", sessionId);  // Passe o sessionId
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemExportar:
                    intent = new Intent(MenuActivity.this, ExportarActivity.class);
                    intent.putExtra("sessionId", sessionId);  // Passe o sessionId
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemFaltas:
                    intent = new Intent(MenuActivity.this, FaltasActivity.class);
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

        getMenuData();
    }

    private void getMenuData() {
        String todayDate = getTodayDateInSqlFormat();
        boolean fromApp = true; // Adicione o parâmetro fromApp
        Call<ResponseBody> call = apiService.getMenuData(todayDate, sessionId, fromApp);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                loadingProgressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        Log.d("MenuActivity", "JSON Response: " + jsonResponse);
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.has("usuario_id") && jsonObject.has("arranchamentosHoje")) {
                            int usuarioId = jsonObject.getInt("usuario_id");
                            JSONArray arranchamentosHoje = jsonObject.getJSONArray("arranchamentosHoje");
                            int faltas = jsonObject.getInt("faltasUsuario");
                            displayMenuData(usuarioId, arranchamentosHoje, faltas);
                        } else {
                            Log.e("MenuActivity", "JSON inesperado: " + jsonResponse);
                            Toast.makeText(MenuActivity.this, "Erro ao processar dados: JSON inesperado", Toast.LENGTH_SHORT).show();
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
                loadingProgressBar.setVisibility(View.GONE);
                Log.e("MenuActivity", "Erro de rede", t);
                Toast.makeText(MenuActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayMenuData(int usuarioId, JSONArray arranchamentosHoje, int faltas) throws JSONException {
        // Construir a string de refeições
        StringBuilder refeicoesBuilder = new StringBuilder();
        if (arranchamentosHoje.length() == 0) {
            refeicoesBuilder.append("Nenhuma refeição hoje.\n");
        } else {
            for (int i = 0; i < arranchamentosHoje.length(); i++) {
                String refeicao = arranchamentosHoje.getString(i);
                if (!refeicoesBuilder.toString().contains(refeicao)) {
                    refeicoesBuilder.append(refeicao).append("\n");
                }
            }
        }

        textViewRefeicoes.setText(refeicoesBuilder.toString());

        // Gerar e exibir o QR code
        Bitmap qrCodeBitmap = generateQRCode(String.valueOf(usuarioId));
        if (qrCodeBitmap != null) {
            imageViewQR.setImageBitmap(qrCodeBitmap);
        }

        // Exibir o número de faltas acumuladas
        String faltasText = "Faltas no rancho até " + getFormattedDate() + ": " + faltas;
        textViewFaltas.setText(faltasText);
    }

    // Método auxiliar para formatar a data de ontem
    private String getFormattedDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(calendar.getTime());
    }

    private Bitmap generateQRCode(String text) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 600, 600);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getTodayDateInSqlFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void logout() {
        Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
