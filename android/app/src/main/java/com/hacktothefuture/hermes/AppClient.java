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
    public static final String API_URL = "http://137.22.189.79:8888";


    public interface MyApp {
        @GET("/get_messages")
        void getBoards(@Query("lat") float lat, @Query("lon") float lon, Callback<List<Board>> cb);

        @POST("/send_message")
        void createBoard(@Body CreateBoardBundle bundle, Callback<String> cb);

        @POST("/append_message")
        void writeMessage(@Body WriteMessageBundle bundle, Callback<String> cb);
    }

}
