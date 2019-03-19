 package com.example.baibu.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.baibu.coolweather.gson.Forecast;
import com.example.baibu.coolweather.gson.LifeStyle;
import com.example.baibu.coolweather.gson.Weather;
import com.example.baibu.coolweather.service.AutoUpdateService;
import com.example.baibu.coolweather.util.HttpUtil;
import com.example.baibu.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

 public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private LinearLayout lifeStyleLayout;
    private TextView lifeStyleText;
    private TextView lifeIntroText;
    private TextView lifeDescText;
    private ImageView bingPicImg;
    private SwipeRefreshLayout swipeRefresh;
    private Button dogHomeButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        swipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        weatherLayout = (ScrollView)findViewById(R.id.weather_layout);
        titleCity = (TextView)findViewById(R.id.title_city);
        titleUpdateTime = (TextView)findViewById(R.id.title_update_time);
        degreeText = (TextView)findViewById(R.id.degree_text);
        weatherInfoText = (TextView)findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout)findViewById(R.id.forecast_layout);
        lifeStyleLayout = (LinearLayout)findViewById(R.id.life_style_layout);
        lifeStyleText =(TextView)findViewById(R.id.life_style);
        lifeIntroText = (TextView)findViewById(R.id.life_intro);
        lifeDescText = (TextView)findViewById(R.id.life_desc);
        bingPicImg = (ImageView)findViewById(R.id.bing_pic_img);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        dogHomeButton = (Button)findViewById(R.id.nav_button);


        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this,R.color.colorPrimary));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic",null);
        if (bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic();
        }


        String weatherString = prefs.getString("weather",null);
        final String weatherId;
        if (weatherString!=null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(weatherId);
        }


        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });


        dogHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });


    }
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取图片失败",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                Log.e("bing", "onResponse: "+bingPic );
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                        WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                        bingPicImg.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
    public void requestWeather(final String weatherId){
        String weatherUrl = "https://free-api.heweather.com/s6/weather?" +
                "key=59f0359e3dab4fb9b032623d83b5f409&location="+weatherId;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气失败",
                                Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                    WeatherActivity.this
                            ).edit();
                            editor.putString("weather",responseText);

                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

        loadBingPic();
    }
    private void showWeatherInfo(Weather weather) {
        if (weather != null && "ok".equals(weather.status)) {
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);

            String cityName = weather.basic.location;
            String updateTime = weather.update.updateTime.split(" ")[1];
            String degree = weather.now.temperature + "℃";
            String weatherInfo = weather.now.info;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            forecastLayout.removeAllViews();
            lifeStyleLayout.removeAllViews();
            for (Forecast forecast : weather.forecastList) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                        forecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.info);
                maxText.setText(forecast.max);
                minText.setText(forecast.min);
                forecastLayout.addView(view);
            }
            for (LifeStyle lifeStyle : weather.lifeStyleList) {
                View view = LayoutInflater.from(this).inflate(R.layout.lifestyle_item,
                        lifeStyleLayout, false);
                TextView typeText = (TextView) view.findViewById(R.id.life_style);
                TextView lifeIntroText = (TextView) view.findViewById(R.id.life_intro);
                TextView lifeDescText = (TextView) view.findViewById(R.id.life_desc);

                if (lifeStyle.type.equals("comf")) {
                    typeText.setText("舒适度指数");
                }
                if (lifeStyle.type.equals("cw")) {
                    typeText.setText("洗车指数");
                }
                if (lifeStyle.type.equals("drsg")) {
                    typeText.setText("穿衣指数");
                }
                if (lifeStyle.type.equals("flu")) {
                    typeText.setText("感冒指数");
                }
                if (lifeStyle.type.equals("sport")) {
                    typeText.setText("运动指数");
                }
                if (lifeStyle.type.equals("air")) {
                    typeText.setText("空气污染扩散条件指数");
                }
                if (lifeStyle.type.equals("trav")) {
                    typeText.setText("旅游指数");
                }
                if (lifeStyle.type.equals("uv")) {
                    typeText.setText("紫外线指数");
                }


                lifeIntroText.setText(lifeStyle.feel);
                lifeDescText.setText(lifeStyle.advise);
                lifeStyleLayout.setVisibility(View.VISIBLE);
                lifeStyleLayout.addView(view);
            }
        }else{
            Toast.makeText(this,"加载天气失败",Toast.LENGTH_SHORT).show();
        }
    }
}
