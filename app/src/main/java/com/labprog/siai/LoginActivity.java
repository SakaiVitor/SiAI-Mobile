package com.labprog.siai;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.exercicion_labprogiii.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.exercicion_labprogiii.R.layout.activity_login);

        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        Log.d(TAG, "Attempting login with email: " + email);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Usuario> call = apiService.login(email, password);
        call.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                Log.d(TAG, "onResponse: " + response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();
                    Log.d(TAG, "Login bem-sucedido: " + usuario.getEmail());
                    Toast.makeText(LoginActivity.this, "Login bem-sucedido", Toast.LENGTH_SHORT).show();
                    // Navegue para a próxima Activity ou salve informações do usuário
                } else {
                    Log.d(TAG, "Falha no login: " + response.message());
                    Toast.makeText(LoginActivity.this, "Falha no login", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e(TAG, "Erro de comunicação: ", t);
                Toast.makeText(LoginActivity.this, "Erro de comunicação", Toast.LENGTH_SHORT).show();
            }
        });
    }
}