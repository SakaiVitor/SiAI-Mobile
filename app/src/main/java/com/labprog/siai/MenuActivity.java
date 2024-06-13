package com.labprog.siai;

import static android.app.ProgressDialog.show;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MenuActivity extends AppCompatActivity {

    private ApiService apiService;
    private String sessionId;
    private String userId;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private RelativeLayout loadingProgressBar;
    private List<String> arranchamentosHoje;
    private String qrCodeData;
    private int faltasUsuario;
    private List<String> faltasLista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionId = getIntent().getStringExtra("sessionId");
        userId = getIntent().getStringExtra("userId");


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        loadingProgressBar = findViewById(R.id.loader);

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
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemExportar:
                    intent = new Intent(MenuActivity.this, ExportarActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    break;
                case R.id.itemFaltas:
                    intent = new Intent(MenuActivity.this, FaltasActivity.class);
                    intent.putExtra("sessionId", sessionId);
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

        checkIfAdmin();
        loadMenuData();
    }

    private void checkIfAdmin() {
        System.out.println(userId);
        Call<ResponseBody> call = apiService.isAdmin(userId);
        call.enqueue(new Callback<ResponseBody>() {
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);

                        // Verifique se a chave "isAdmin" está presente no JSON
                        if (jsonObject.has("isAdmin")) {
                            boolean isAdmin = jsonObject.getBoolean("isAdmin");
                            if (!isAdmin) {
                                Menu menu = navigationView.getMenu();
                                menu.findItem(R.id.itemFaltas).setVisible(false);
                            }
                        } else {
                            Toast.makeText(MenuActivity.this, "Resposta JSON não contém isAdmin", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MenuActivity.this, "Erro ao processar dados de admin", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MenuActivity.this, "Erro ao verificar admin: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "Erro de rede ao verificar admin", Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    private void loadMenuData() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        String todayDate = getTodayDateInSqlFormat();
        boolean fromApp = true;
        Call<ResponseBody> call = apiService.getMenuData(todayDate, sessionId, fromApp);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        System.out.println("JSON Response: " + jsonResponse); // Debugging
                        JSONObject jsonObject = new JSONObject(jsonResponse);

                        // Check for the presence of required fields
                        if (jsonObject.has("usuario_id") && jsonObject.has("arranchamentosHoje") && jsonObject.has("faltasUsuario") && jsonObject.has("faltasLista")) {

                            // Parse arranchamentosHoje
                            Set<String> arranchamentosHojeSet = new HashSet<>();
                            JSONArray arranchamentosHojeArray = jsonObject.getJSONArray("arranchamentosHoje");
                            for (int i = 0; i < arranchamentosHojeArray.length(); i++) {
                                arranchamentosHojeSet.add(arranchamentosHojeArray.getString(i));
                            }

                            // Mapping names
                            Map<String, String> nameMapping = new HashMap<>();
                            nameMapping.put("cafe", "Café");
                            nameMapping.put("almoco", "Almoço");
                            nameMapping.put("janta", "Janta");
                            nameMapping.put("ceia", "Ceia");

                            // Replace names
                            arranchamentosHoje = new ArrayList<>();
                            for (String arranchamento : arranchamentosHojeSet) {
                                String displayName = nameMapping.get(arranchamento);
                                if (displayName != null) {
                                    arranchamentosHoje.add(displayName);
                                }
                            }

                            // Parse other fields
                            qrCodeData = String.valueOf(jsonObject.getInt("usuario_id"));
                            faltasUsuario = jsonObject.getInt("faltasUsuario");
                            faltasLista = new ArrayList<>();
                            JSONArray faltasArray = jsonObject.getJSONArray("faltasLista");
                            for (int i = 0; i < faltasArray.length(); i++) {
                                faltasLista.add(faltasArray.getString(i));
                            }

                            setupViewPagerAndTabs();
                        } else {
                            Toast.makeText(MenuActivity.this, "Erro ao processar dados: JSON inesperado", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MenuActivity.this, "Erro ao processar dados", Toast.LENGTH_SHORT).show();
                        e.printStackTrace(); // Print stack trace for debugging
                    }
                } else {
                    Toast.makeText(MenuActivity.this, "Erro ao obter dados: " + response.message(), Toast.LENGTH_SHORT).show();
                }
                loadingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingProgressBar.setVisibility(View.GONE);
                Toast.makeText(MenuActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
                t.printStackTrace(); // Print stack trace for debugging
            }
        });
    }

    private void setupViewPagerAndTabs() {
        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), this);

        ArranchamentosFragment arranchamentosFragment = new ArranchamentosFragment();
        QRCodeFragment qrCodeFragment = new QRCodeFragment();
        FaltasFragment faltasFragment = new FaltasFragment();

        adapter.addFragment(arranchamentosFragment, "Arranchamentos", R.drawable.qr);
        adapter.addFragment(qrCodeFragment, "QR Code", R.drawable.ar);
        adapter.addFragment(faltasFragment, "Faltas", R.drawable.falta);

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(adapter.getTabView(i));
            }
        }
    }

    public List<String> getfaltasLista() {
        return faltasLista;
    }

    public List<String> getArranchamentosHoje() {
        return arranchamentosHoje;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public int getFaltasUsuario() {
        return faltasUsuario;
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
