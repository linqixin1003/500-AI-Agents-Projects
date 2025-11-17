package com.diabeat.data.api

import com.diabeat.data.model.OpenFoodFactsResponse
import com.diabeat.data.model.USDAFoodResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * USDA FoodData Central API
 * 文档: https://fdc.nal.usda.gov/api-guide.html
 */
interface USDAFoodDataApi {
    
    /**
     * 根据GTIN/UPC条形码搜索食品
     * @param gtinUpc 条形码（GTIN/UPC）
     * @param apiKey USDA API Key（免费申请）
     */
    @GET("v1/foods/search")
    suspend fun searchByBarcode(
        @Query("query") gtinUpc: String,
        @Query("dataType") dataType: String = "Branded",  // 品牌食品
        @Query("pageSize") pageSize: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<USDAFoodResponse>
    
    /**
     * 根据食品ID获取详细信息
     */
    @GET("v1/food/{fdcId}")
    suspend fun getFoodById(
        @Path("fdcId") fdcId: Int,
        @Query("api_key") apiKey: String
    ): Response<USDAFoodResponse>
}

/**
 * OpenFoodFacts API
 * 文档: https://wiki.openfoodfacts.org/API
 * 优势: 完全免费，无API Key，全球数据库
 */
interface OpenFoodFactsApi {
    
    /**
     * 根据条形码获取产品信息
     * @param barcode 条形码（EAN-13, UPC-A等）
     */
    @GET("api/v2/product/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>
}

/**
 * DiabEat后端API（可选）
 * 用于缓存和数据增强
 */
interface DiabEatFoodApi {
    
    /**
     * 扫描条形码（后端会调用USDA + OpenFoodFacts，互补验证）
     */
    @GET("food/scan/{barcode}")
    suspend fun scanBarcode(
        @Path("barcode") barcode: String
    ): Response<com.diabeat.data.model.FoodProduct>
    
    /**
     * 记录食品日志
     */
    @retrofit2.http.POST("food/log")
    suspend fun logFood(
        @retrofit2.http.Body request: com.diabeat.data.model.FoodLogRequest
    ): Response<Any>
}
