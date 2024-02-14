package com.meteorcat.mix.constant;

/**
 * 请求响应数据协议
 */
public class Protocols {
    public static final int AUTH_LOGIN = 100; // 客户端推送给服务端请求
    public static final int AUTH_ERROR_BY_EXISTS = 101; // 服务端授权不存在
    public static final int AUTH_ERROR_BY_SECRET = 102; // 服务端推送授权响应失败
    public static final int AUTH_ERROR_BY_OTHER = 103; // 服务端推送异地登录
    public static final int AUTH_LOGIN_SUCCESS = 110; // 登录成功
    public static final int PLAYER_CREATE = 200; // 创建用户
    public static final int PLAYER_EXISTS = 201; // 用户已存在
    public static final int PLAYER_NOT_FOUND = 202; // 用户不存在
    public static final int PLAYER_INFO = 210; // 用户信息
    public static final int SYS_HEARTBEAT = 1; // 心跳包推送
    public static final int SYS_PLAYER_EXISTS = 2; // 玩家存在
    public static final int SYS_PARAM_ERROR = 3; // 参数错误
    public static final int SYS_CHANGE_GOLD = 10; // 修改玩家金币
}
