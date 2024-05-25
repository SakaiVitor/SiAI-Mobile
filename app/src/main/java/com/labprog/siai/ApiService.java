package com.labprog.siai;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {
    @FormUrlEncoded
    @POST("loginServlet")
    Call<Usuario> login(@Field("email") String email, @Field("password") String password);
}