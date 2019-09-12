package com.jimboss.deer.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimboss.deer.common.controller.BaseController;
import com.jimboss.deer.common.domain.ActiveUser;
import com.jimboss.deer.common.domain.DeerConstant;
import com.jimboss.deer.common.domain.router.VueRouter;
import com.jimboss.deer.common.service.RedisService;
import com.jimboss.deer.common.utils.DateUtil;
import com.jimboss.deer.system.domain.Menu;
import com.jimboss.deer.system.manager.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

/**
 * @ClassName MenuController
 * @Description TODO
 * @Author jinyong
 * @DATE 2019/7/18 12:08
 * @Version 1.0
 **/

@Slf4j
@Validated
@RestController
@RequestMapping("/menu")
public class MenuController extends BaseController {

    @Autowired
    private UserManager userManager;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("/{username}")
    public ArrayList<VueRouter<Menu>> getUserRouters(@NotBlank(message = "{required}") @PathVariable String username) {
        return this.userManager.getUserRouters(username);
    }

    @DeleteMapping("kickout/{id}")
    @RequiresPermissions("user:kickout")
    public void kickout(@NotBlank(message = "{required}") @PathVariable String id) throws Exception {
        String now = DateUtil.formatFullTime(LocalDateTime.now());
        Set<String> userOnlineStringSet = redisService.zrangeByScore(DeerConstant.ACTIVE_USERS_ZSET_PREFIX, now, "+inf");
        ActiveUser kickoutUser = null;
        String kickoutUserString = "";
        for (String userOnlineString : userOnlineStringSet) {
            ActiveUser activeUser = mapper.readValue(userOnlineString, ActiveUser.class);
            if (StringUtils.equals(activeUser.getId(), id)) {
                kickoutUser = activeUser;
                kickoutUserString = userOnlineString;
            }
        }
        if (kickoutUser != null && StringUtils.isNotBlank(kickoutUserString)) {
            // 删除 zset中的记录
            redisService.zrem(DeerConstant.ACTIVE_USERS_ZSET_PREFIX, kickoutUserString);
            // 删除对应的 token缓存
            redisService.del(DeerConstant.TOKEN_CACHE_PREFIX + kickoutUser.getToken() + "." + kickoutUser.getIp());
        }
    }
}
