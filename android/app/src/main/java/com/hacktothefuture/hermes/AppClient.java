package com.hacktothefuture.hermes;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by ldaniels on 4/25/15.
 */
public class AppClient {

    public interface MyApp {
        @GET("/get_messages")
        void getMessages(@Query("lat") float lat, @Query("lon") float lon, Callback<List<Message>> cb);

        @POST("/send_message")
        void sendMessage(@Body PostBundle bundle, Callback<Void> cb);
    }

}
