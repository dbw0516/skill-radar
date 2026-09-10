package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册成功后返回给前端的用户视图——不带 passwordHash。
 * 注意：这不是真正的会话令牌，前端拿到这个对象后自己存在本地当"已登录"标记，
 * 后续请求把 userId 带上就行。等系统要接真实鉴权时（比如防止随便传别人的 userId），
 * 再升级成 JWT / session，现在先满足"能注册登录、数据能落库"这个最基本的需求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private Long id;
    private String email;
    private String nickname;
    private Long majorId;
    private Long targetCategoryId;
    private String location;
    private String targetLocation;
}
