package com.qiyiqi.aicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.qiyiqi.aicode.constant.AppConstant;
import com.qiyiqi.aicode.exception.BusinessException;
import com.qiyiqi.aicode.exception.ErrorCode;
import com.qiyiqi.aicode.exception.ThrowUtils;
import com.qiyiqi.aicode.mapper.AppMapper;
import com.qiyiqi.aicode.model.dto.app.AppQueryRequest;
import com.qiyiqi.aicode.model.entity.App;
import com.qiyiqi.aicode.model.entity.User;
import com.qiyiqi.aicode.model.vo.AppVO;
import com.qiyiqi.aicode.model.vo.UserVO;
import com.qiyiqi.aicode.service.AppService;
import com.qiyiqi.aicode.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author qiyiqi
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Override
    public void validApp(App app, boolean add) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String appName = app.getAppName();
        String initPrompt = app.getInitPrompt();
        // 创建时，初始化的 prompt 必须填写
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        }
        if (StrUtil.isNotBlank(appName)) {
            ThrowUtils.throwIf(appName.length() > AppConstant.APP_NAME_MAX_LENGTH,
                    ErrorCode.PARAMS_ERROR, "应用名称不能超过 " + AppConstant.APP_NAME_MAX_LENGTH + " 个字符");
        }
        if (StrUtil.isNotBlank(initPrompt)) {
            ThrowUtils.throwIf(initPrompt.length() > 4000, ErrorCode.PARAMS_ERROR, "初始化 prompt 过长");
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("cover", cover)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .like("appName", appName)
                .like("initPrompt", initPrompt)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public QueryWrapper getGoodAppQueryWrapper(AppQueryRequest appQueryRequest) {
        // 精选应用：优先级达到阈值的应用（由管理员设置 priority 决定）
        return getQueryWrapper(appQueryRequest)
                .ge("priority", AppConstant.GOOD_APP_MIN_PRIORITY);
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 填充创建者信息
        User creator = userService.getById(app.getUserId());
        appVO.setUser(userService.getUserVO(creator));
        return appVO;
    }

    @Override
    public Page<AppVO> getAppVOPage(Page<App> appPage) {
        if (appPage == null) {
            return null;
        }
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        appVOPage.setRecords(getAppVOList(appPage.getRecords()));
        return appVOPage;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量查询创建者信息，避免 N + 1
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<User> userList = userService.listByIds(userIds);
            if (CollUtil.isNotEmpty(userList)) {
                userVOMap = userList.stream()
                        .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));
            }
        }
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(finalUserVOMap.get(app.getUserId()));
            return appVO;
        }).collect(Collectors.toList());
    }
}
