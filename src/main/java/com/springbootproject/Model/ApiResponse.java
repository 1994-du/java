package com.springbootproject.Model;

/**
 * 统一API响应结构
 */
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
     * 总条数
     */
    private Long total;
    
    // Getters and Setters
    public Number getCode() {
        return code;
    }
    
    public void setCode(Number code) {
        this.code = code;
    }
    
    public String getMsg() {
        return msg;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Long getTotal() {
        return total;
    }
    
    public void setTotal(Long total) {
        this.total = total;
    }
    
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
     * 创建成功响应（带总条数）
     */
    public static <T> ApiResponse<T> success(String message, T data, Long total) {
        ApiResponse<T> response = success(message, data);
        response.setTotal(total);
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