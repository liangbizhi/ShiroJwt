package com.wang.config.shiro;

import com.wang.config.shiro.jwt.*;
import com.wang.mapper.*;
import com.wang.model.*;
import com.wang.model.common.Constant;
import com.wang.util.*;
import com.wang.util.common.StringUtil;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.*;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义Realm
 * @author Wang926454
 * @date 2018/8/30 14:10
 */
@Service
public class UserRealm extends AuthorizingRealm {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtFilter.class);

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Autowired
    public UserRealm(UserMapper userMapper, RoleMapper roleMapper, PermissionMapper permissionMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    /**
     * 大坑，必须重写此方法，不然Shiro会报错
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 只有当需要检测用户权限的时候才会调用此方法，例如checkRole,checkPermission之类的
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LOGGER.info("[Shiro|Realm] doGetAuthorizationInfo(PrincipalCollection)");
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        String account = JwtUtil.getClaim(principals.toString(), Constant.ACCOUNT);
        UserDto userDto = new UserDto();
        userDto.setAccount(account);
        // 查询用户角色
        List<RoleDto> roleDtos = roleMapper.findRoleByUser(userDto);
        for (int i = 0, roleLen = roleDtos.size(); i < roleLen; i++) {
            RoleDto roleDto = roleDtos.get(i);
            // 添加角色
            simpleAuthorizationInfo.addRole(roleDto.getName());
            // 根据用户角色查询权限
            List<PermissionDto> permissionDtos = permissionMapper.findPermissionByRole(roleDto);
            for (int j = 0, perLen = permissionDtos.size(); j < perLen; j++) {
                PermissionDto permissionDto = permissionDtos.get(j);
                // 添加权限
                simpleAuthorizationInfo.addStringPermission(permissionDto.getPerCode());
            }
        }
        return simpleAuthorizationInfo;
    }

    /**
     * 默认使用此方法进行用户名正确与否验证，错误抛出异常即可。
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken auth) throws AuthenticationException {
        LOGGER.info("[Shiro|Realm] doGetAuthenticationInfo(AuthenticationToken)");
        String token = (String) auth.getCredentials();
        // 解密获得account，用于和数据库进行对比
        String account = JwtUtil.getClaim(token, Constant.ACCOUNT);
        // 帐号为空
        if (StringUtil.isBlank(account)) {
            throw new AuthenticationException("Token中帐号为空(The account in Token is empty.)");
        }
        // 查询用户是否存在
        UserDto userDto = new UserDto();
        userDto.setAccount(account);
        userDto = userMapper.selectOne(userDto);
        if (userDto == null) {
            throw new AuthenticationException("该帐号不存在(The account does not exist.)");
        }
        // 开始认证，要AccessToken认证通过，且Redis中存在RefreshToken，且两个Token时间戳一致
        if(JwtUtil.verify(token) && RedisUtil.exists(Constant.PREFIX_SHIRO_REFRESH_TOKEN + account)){
            // 获取RefreshToken的时间戳
            String currentTimeMillisRedis = RedisUtil.getObject(Constant.PREFIX_SHIRO_REFRESH_TOKEN + account).toString();
            // 获取AccessToken时间戳，与RefreshToken的时间戳对比
            if(JwtUtil.getClaim(token, Constant.CURRENT_TIME_MILLIS).equals(currentTimeMillisRedis)){
                return new SimpleAuthenticationInfo(token, token, "userRealm");
            }
        }
        throw new AuthenticationException("Token已过期(Token expired or incorrect.)");
    }
}
