package com.springbootproject.Model;

import lombok.Data;

/**
 * 统一API响应结构
 */
@Data
public class ApiResponse<T> {
    
    /**
     * 响应状态码
     */
    private Number code;
    
    /**
     * 响应消息
     */
    private String msg;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMsg(message);
        response.setData(data);
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMsg(message);
        return response;
    }
    
    /**
     * 创建失败响应（带数据）
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        ApiResponse<T> response = error(message);
        response.setData(data);
        return response;
    }
}