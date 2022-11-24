package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOW_USER_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据 id 修改 关注/取消关注
     * @param id
     * @param isFollow
     * @return
     */
    @Override
    public Result followUser(Long id, boolean isFollow) {
        //1.获取该用户 id
        Long userId = UserHolder.getUser().getId();
        //2.获取 要判断是否关注的用户id
        Long followUserId = userService.getById(id).getId();
        //3.判断 关注 还是 取消关注
        String key = FOLLOW_USER_KEY + userId;
        if (isFollow){
            //关注逻辑
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            //存入Redis
            if (isSuccess){
                stringRedisTemplate.opsForSet().add(key, id.toString());
            }

        }else{
            //取消关注的逻辑
            //数据库中移除
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<Follow>();
            queryWrapper.eq(Follow::getUserId,userId).eq(Follow::getFollowUserId,followUserId);
            boolean isSuccess = remove(queryWrapper);
            //Redis中移除
            if (isSuccess){
                stringRedisTemplate.opsForSet().remove(key, id.toString());
            }
        }

        return Result.ok();
    }

    /**
     * 根据 id 判断是否关注
     * @param id
     * @return
     */
    @Override
    public Result notFollow(Long id) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //从数据库中查询
        Integer count = query().eq("user_id", userId)
                .eq("follow_user_id", id)
                .count();
        //判断是否已经关注
        return Result.ok(count > 0);
    }

    /**
     * 共同关注查询
     * @param id
     * @return
     */
    @Override
    public Result followCommon(Long id) {
        //1.获取当前用户id key
        String key1 = FOLLOW_USER_KEY + UserHolder.getUser().getId().toString();
        //2.获取查询用户id key
        String key2 = FOLLOW_USER_KEY + id.toString();
        //3.从Redis中求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        //4.转换为集合
        if (ObjectUtils.isEmpty(intersect)){
            return Result.ok(Collections.emptyList());
        }
        List<Long> idList = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //查询用户
        List<UserDTO> users =
                userService.listByIds(idList)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
