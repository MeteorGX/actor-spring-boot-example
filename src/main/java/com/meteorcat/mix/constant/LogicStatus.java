package com.meteorcat.mix.constant;

/**
 * 访问状态状态
 */
public class LogicStatus {

    /**
     * 无状态
     */
    public static final int None = 0;


    /**
     * 已授权状态
     */
    public static final int Authorized = 1;

    /**
     * 程序内部传递, 不对外暴露
     */
    public static final int Program = 2;


    /**
     * 游戏中状态
     */
    public static final int Gaming = 3;
}
