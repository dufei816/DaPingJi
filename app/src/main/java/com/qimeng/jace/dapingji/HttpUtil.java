package com.qimeng.jace.dapingji;

import android.util.Log;

import com.qimeng.jace.dapingji.entity.Buy;
import com.qimeng.jace.dapingji.entity.Commodity;
import com.qimeng.jace.dapingji.entity.Image;
import com.qimeng.jace.dapingji.entity.User;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class HttpUtil {

    private static final String URL = "http://112.74.160.179/gjj_weixin/portal/inf/";
    private static final int OUT_TIME = 10;
    private static final String TAG = "Http";


    private static HttpUtil mHttpUtil;
    private Http http;


    public Http getHttp() {
        return http;
    }

    public synchronized static HttpUtil getInstance() {
        if (mHttpUtil == null) {
            mHttpUtil = new HttpUtil();
        }
        return mHttpUtil;
    }

    public HttpUtil() {
        init();
    }

    private void init() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(OUT_TIME, TimeUnit.SECONDS);//连接 超时时间
        builder.writeTimeout(OUT_TIME, TimeUnit.SECONDS);//写操作 超时时间
        builder.readTimeout(OUT_TIME, TimeUnit.SECONDS);//读操作 超时时间
        builder.retryOnConnectionFailure(true);//错误重连

        Interceptor tor = chain -> {
            Request request = chain.request();
            Log.e(TAG, request.url().toString());
            return chain.proceed(request);
        };

        builder.addInterceptor(tor);
        Retrofit retrofit = new Retrofit.Builder()
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(URL)
                .build();
        http = retrofit.create(Http.class);
    }

    public interface Http{
        /**
         * 商品接口
         * @return
         */
        @GET("getLp.jsp")
        Observable<Commodity> getLp();

        /**
         * 广告图接口
         * @return
         */
        @GET("getGg.jsp")
        Observable<Image> getGg();

        /**
         * 获取用户
         * @param code
         * @return
         */
        @GET("getUserDp.jsp")
        Observable<User> getUserDp(@Query("code") String code);


        /**
         * 礼品兑换
         * @param jqbh
         * @param lpid
         * @param jf
         * @param userid
         * @return
         */
        @GET("getLpdh.jsp")
        Observable<Buy> getLpdh(@Query("jqbh") String jqbh, @Query("lpid") String lpid, @Query("jf") String jf, @Query("userid") String userid);

    }

}
