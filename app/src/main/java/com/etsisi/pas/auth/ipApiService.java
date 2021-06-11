package com.etsisi.pas.auth;

import com.etsisi.pas.auth.models.Iplocation;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ipApiService {

    @GET("/{ip}")
    Call<Iplocation> getLocation(
            @Path("ip") String ip,
            @Query("access_key") String username
    );


}
