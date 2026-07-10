package com.shopper.service;

import java.util.Map;

public interface AuthService {

    /**
     * 用户登录，返回 { token, user }
     */
    Map<String, Object> login(String username, String password);
}
