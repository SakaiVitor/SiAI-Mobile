package com.labprog.siai;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.57.251:8080/";
    private static Retrofit retrofit = null;

    private static Set<String> cookies = new HashSet<>();

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request.Builder builder = chain.request().newBuilder();
                            for (String cookie : cookies) {
                                builder.addHeader("Cookie", cookie);
                            }
                            return chain.proceed(builder.build());
                        }
                    })
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response response = chain.proceed(chain.request());
                            if (!response.headers("Set-Cookie").isEmpty()) {
                                for (String header : response.headers("Set-Cookie")) {
                                    cookies.add(header);
                                }
                            }
                            return response;
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
