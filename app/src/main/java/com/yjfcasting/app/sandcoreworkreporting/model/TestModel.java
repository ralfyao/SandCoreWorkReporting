package com.yjfcasting.app.sandcoreworkreporting.model;

import com.google.gson.annotations.SerializedName;

public class TestModel {
    @SerializedName("id")
    public Integer id = null;
    @SerializedName("title")
    public String title = null;
    @SerializedName("price")
    public Integer price = null;
    @SerializedName("discountPercentage")
    public Double discountPercentage = null;
    @SerializedName("rating")
    public Double rating = null;
}
