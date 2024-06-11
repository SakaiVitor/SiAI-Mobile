package com.labprog.siai;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("loginServlet")
    Call<ResponseBody> login(@Field("email") String email, @Field("password") String password, @Field("fromApp") String fromApp);

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

    @FormUrlEncoded
    @POST("export")
    Call<Void> export(
            @Field("dataInicio") String dataInicio,
            @Field("dataFim") String dataFim,
            @Field("turma") String turma,
            @Field("pelotao") String pelotao
    );

    @GET("menuServlet")
    Call<ResponseBody> getMenuData(@Query("fromApp") String fromApp, @Query("sessionId") String sessionId);

    @FormUrlEncoded
    @POST("arranchamento")
    Call<ResponseBody> enviarArranchamento(
            @Field("arranchamento[]") String[] arranchamentos,
            @Field("fromApp") String fromApp,
            @Field("lastDateDisplayed") String lastDateDisplayed
    );

    @GET("arranchamento")
    Call<ResponseBody> getArranchamentoData(@Query("fromApp") String fromApp, @Query("sessionId") String sessionId);
}

