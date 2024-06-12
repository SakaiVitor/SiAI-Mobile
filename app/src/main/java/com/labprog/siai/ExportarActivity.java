package com.labprog.siai;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ExportarActivity extends AppCompatActivity {
    private EditText dataInicioField, dataFinalField, turmaField, pelotaoField;
    private Button exportarButton;
    private String sessionId;
    private String userId;
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

        sessionId = getIntent().getStringExtra("sessionId");
        userId = getIntent().getStringExtra("userId");

        navigationView.setNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.itemMenu:
                    intent = new Intent(ExportarActivity.this, MenuActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemPreencher:
                    intent = new Intent(ExportarActivity.this, ArranchamentoActivity.class);
                    intent.putExtra("sessionId", sessionId);  // Passe o sessionId
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemExportar:
                    intent = new Intent(ExportarActivity.this, ExportarActivity.class);
                    intent.putExtra("sessionId", sessionId);  // Passe o sessionId
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemFaltas:
                    intent = new Intent(ExportarActivity.this, FaltasActivity.class);
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
        Call<ResponseBody> call = apiService.export(dataInicio, dataFinal, turma, pelotao);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Save the file
                    boolean isFileSaved = saveFile(response.body(), dataInicio, dataFinal, turma, pelotao);
                    if (isFileSaved) {
                        Toast.makeText(ExportarActivity.this, "Export bem-sucedido! Verifique seus Downloads :)", Toast.LENGTH_SHORT).show();
                        // Redirecionar para MenuActivity
                        Intent intent = new Intent(ExportarActivity.this, MenuActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ExportarActivity.this, "Erro ao salvar o arquivo", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ExportarActivity.this, "Falha ao exportar " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                exportarButton.setEnabled(true);
                Log.e(TAG, "Erro de comunicação: ", t);
                Toast.makeText(ExportarActivity.this, "Erro de comunicação", Toast.LENGTH_SHORT).show();
            }

        });
    }
    private static final String CHANNEL_ID = "download_channel";

    private boolean saveFile(ResponseBody body, String dataInicio, String dataFim, String turma, String pelotao) {
        Date date = new Date();
        long timestamp = date.getTime();
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String nomeArquivo = "T" + turma + "_P" + pelotao + "º_(" + dataInicio.replaceAll("[^a-zA-Z0-9]", "-") + "&" + dataFim.replaceAll("[^a-zA-Z0-9]", "-") + ")" + timestamp + ".xlsx";
            File file = new File(path, nomeArquivo);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);
                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                }

                outputStream.flush();
                showDownloadNotification(file);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Erro ao salvar o arquivo", e);
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar o arquivo", e);
            return false;
        }
    }

    private void showDownloadNotification(File file) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificações para downloads concluídos");
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }

        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.sakai)  // Verifique se este ícone existe no res/drawable
                .setContentTitle("Download concluído")
                .setContentText("Clique para abrir o arquivo")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        Log.d(TAG, "Sending notification");
        notificationManager.notify(1, builder.build());
        Log.d(TAG, "Notification sent");
    }



    private void logout() {
        // Limpar sessão ou qualquer dado de login aqui
        Intent intent = new Intent(ExportarActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}

