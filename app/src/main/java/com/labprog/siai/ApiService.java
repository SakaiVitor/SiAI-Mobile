package com.labprog.siai;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {
    @FormUrlEncoded
    @POST("loginServlet")
    Call<Usuario> login(@Field("email") String email, @Field("password") String password);

    @FormUrlEncoded
    @POST("cadastro")
    Call<Void> register(
            @Field("nome") String nome,
            @Field("nome_guerra") String nomeDeGuerra,
            @Field("matricula") String matricula,
            @Field("turma") String turma,
            @Field("pelotao") String pelotao,
            @Field("email") String email,
            @Field("password") String senha
    );
}