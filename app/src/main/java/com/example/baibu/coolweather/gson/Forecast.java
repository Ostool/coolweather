package com.example.baibu.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Forecast {
    public String date;

    @SerializedName("cond_text_d")
    public String info;

   @SerializedName("tmp_max")
    public String max;

   @SerializedName("tmp_min")
    public String min;



}
