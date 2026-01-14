package com.sky.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }


    // 移除当前线程的线程局部变量
    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
