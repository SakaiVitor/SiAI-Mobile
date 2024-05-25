package com.labprog.siai;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText nicknameEditText;
    private EditText registrationEditText;
    private EditText classEditText;
    private EditText platoonEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private LinearLayout strengthMeter;
    private TextView strengthText;
    private View strengthBarFill;
    private Button registerButton;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.labprog.siai.R.layout.activity_register);

        nameEditText = findViewById(R.id.register_name);
        nicknameEditText = findViewById(R.id.register_nickname);
        registrationEditText = findViewById(R.id.register_registration);
        classEditText = findViewById(R.id.register_class);
        platoonEditText = findViewById(R.id.register_platoon);
        emailEditText = findViewById(R.id.register_email);
        passwordEditText = findViewById(R.id.register_password);
        strengthMeter = findViewById(R.id.strengthMeter);
        strengthText = findViewById(R.id.strengthText);
        strengthBarFill = findViewById(R.id.strengthBarFill);
        registerButton = findViewById(R.id.register_button);

        // Adiciona o TextWatcher ao campo de senha
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Não utilizado
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                verificarForcaSenha(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Não utilizado
            }
        });
    }

    private void verificarForcaSenha(String senha) {
        // Verifica a força da senha
        int forca = 0;
        if (senha.length() >= 8) forca++;
        if (senha.matches(".*[a-z].*")) forca++;
        if (senha.matches(".*[A-Z].*")) forca++;
        if (senha.matches(".*[0-9].*")) forca++;
        if (senha.matches(".*[$-/:-?{-~!\"^_`\\[\\]].*")) forca++;

        // Atualiza a exibição da força da senha
        switch (forca) {
            case 0:
            case 1:
                strengthText.setText("Muito Fraca");
                strengthBarFill.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                strengthBarFill.getLayoutParams().width = (int) (0.2 * strengthMeter.getWidth());
                break;
            case 2:
                strengthText.setText("Fraca");
                strengthBarFill.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
                strengthBarFill.getLayoutParams().width = (int) (0.4 * strengthMeter.getWidth());
                break;
            case 3:
                strengthText.setText("Média");
                strengthBarFill.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                strengthBarFill.getLayoutParams().width = (int) (0.6 * strengthMeter.getWidth());
                break;
            case 4:
                strengthText.setText("Forte");
                strengthBarFill.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                strengthBarFill.getLayoutParams().width = (int) (0.8 * strengthMeter.getWidth());
                break;
            case 5:
                strengthText.setText("Muito Forte");
                strengthBarFill.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                strengthBarFill.getLayoutParams().width = (int) (1.0 * strengthMeter.getWidth());
                break;
        }
        strengthMeter.setVisibility(View.VISIBLE);
    }

    public void onRegisterClicked(View view) {
        String name = nameEditText.getText().toString().trim();
        String nickname = nicknameEditText.getText().toString().trim();
        String registration = registrationEditText.getText().toString().trim();
        String classNum = classEditText.getText().toString().trim();
        String platoon = platoonEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (name.isEmpty() || nickname.isEmpty() || registration.isEmpty() || classNum.isEmpty() || platoon.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(RegisterActivity.this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        registerButton.setEnabled(false);

        // Crie uma instância do serviço de API e faça a chamada de registro
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Void> call = apiService.register(name, nickname, registration, classNum, platoon, email, password);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                registerButton.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Cadastro realizado com sucesso", Toast.LENGTH_SHORT).show();
                    finish(); // Voltar para a tela de login
                } else {
                    Toast.makeText(RegisterActivity.this, "Falha no cadastro: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                registerButton.setEnabled(true);
                Log.e(TAG, "Erro de comunicação: ", t);
                Toast.makeText(RegisterActivity.this, "Erro de comunicação", Toast.LENGTH_SHORT).show();
            }
        });
    }
}