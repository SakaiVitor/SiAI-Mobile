package com.labprog.siai;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private TextView registerLink;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);
        apiService = ApiClient.getClient().create(ApiService.class);

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }
            login(email, password);
        });

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void login(String email, String password) {
        Call<ResponseBody> call = apiService.login(email, password, "true");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        Usuario usuario = new Usuario(
                                jsonObject.getJSONObject("usuario").getString("nome"),
                                jsonObject.getJSONObject("usuario").getString("email")
                        );
                        String sessionId = jsonObject.getString("sessionId");
                        String userId = jsonObject.getString("userId");
                        System.out.println(userId);

                        Toast.makeText(LoginActivity.this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show();
                        // Redirecionar para MenuActivity
                        Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                        intent.putExtra("sessionId", sessionId);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Erro ao processar JSON", e);
                        Toast.makeText(LoginActivity.this, "Erro ao processar dados", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Falha no login: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("LoginActivity", "Erro: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
