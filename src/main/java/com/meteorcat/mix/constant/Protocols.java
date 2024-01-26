package com.meteorcat.mix.constant;

/**
 * 对应所有协议号
 * 注意:
 * 协议号 % 2 == 0 为请求协议
 * 协议号 % 2 != 0 为响应协议
 */
public class Protocols {

    public static final int PARAM_ERROR = 11; // 参数错误协议: 服务端 -> 客户端

    public static final int LOGIN = 100; // 请求登录: 客户端 -> 服务端

    public static final int SECRET_ERROR = 101; // 授权错误: 服务端 -> 客户端

    public static final int HEARTBEAT = 103; // 心跳包: 服务端 -> 客户端

    public static final int OTHER_LOGIN = 105; // 异地登录: 服务端 -> 客户端


    public static final int CHANGE_SCENE = 121;// 通知客户端切换关卡: 服务端 -> 客户端


}
