package com.qiyiqi.aicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.qiyiqi.aicode.annotation.AuthCheck;
import com.qiyiqi.aicode.common.BaseResponse;
import com.qiyiqi.aicode.common.DeleteRequest;
import com.qiyiqi.aicode.common.IdUtils;
import com.qiyiqi.aicode.common.ResultUtils;
import com.qiyiqi.aicode.constant.AppConstant;
import com.qiyiqi.aicode.constant.UserConstant;
import com.qiyiqi.aicode.exception.BusinessException;
import com.qiyiqi.aicode.exception.ErrorCode;
import com.qiyiqi.aicode.exception.ThrowUtils;
import com.qiyiqi.aicode.model.dto.app.*;
import com.qiyiqi.aicode.model.entity.App;
import com.qiyiqi.aicode.model.entity.User;
import com.qiyiqi.aicode.model.enums.CodeGenTypeEnum;
import com.qiyiqi.aicode.model.vo.AppVO;
import com.qiyiqi.aicode.service.AppService;
import com.qiyiqi.aicode.service.UserService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

import static org.aspectj.asm.internal.CharOperation.concatWith;

/**
 * 应用 控制层。
 *
 * @author qiyiqi
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @GetMapping(value="/chat/gen/code",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        //1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "请求内容不能为空");
        //2.获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        //3.调用服务生成代码
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                    //发送结束时间
                    ServerSentEvent.<String>builder()
                        .event("done")
                        .build()
        ));

    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.DeployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


    // region 用户接口

    /**
     * 创建应用
     *
     * @param appAddRequest 应用创建请求
     * @param request       请求对象
     * @return 新应用 id（字符串形式，避免 18~19 位雪花 id 在前端被截断）
     */
    @PostMapping("/add")
    public BaseResponse<String> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        // 校验参数（initPrompt 必填）
        appService.validApp(app, true);
        // 未填写应用名称时，默认取 initPrompt 的前若干的字符
        if (StrUtil.isBlank(app.getAppName())) {
            String initPrompt = app.getInitPrompt();
            app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(),12)));
        }
        // 默认生成多文件类型
            app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        app.setUserId(loginUser.getId());
        app.setPriority(0);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(String.valueOf(app.getId()));
    }

    /**
     * 更新自己的应用（目前只支持修改应用名称）
     *
     * @param appUpdateRequest 应用更新请求
     * @param request          请求对象
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null
                        || StrUtil.isBlank(appUpdateRequest.getId()),
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long id = IdUtils.parseId(appUpdateRequest.getId(), "id");
        // 校验应用是否存在，且只能修改自己的应用
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 目前只支持修改应用名称，因此应用名称必填
        String appName = appUpdateRequest.getAppName();
        ThrowUtils.throwIf(StrUtil.isBlank(appName), ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        App app = new App();
        app.setId(id);
        app.setAppName(appName);
        appService.validApp(app, false);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除自己的应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求对象
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null
                || StrUtil.isBlank(deleteRequest.getId()), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long id = IdUtils.parseId(deleteRequest.getId(), "id");
        // 校验应用是否存在，且只能删除自己的应用
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情（脱敏）
     *
     * @param id 应用 id
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(@RequestParam("id") String id) {
        long appId = IdUtils.parseId(id, "id");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询我的应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param appQueryRequest 查询请求参数
     * @param request         请求对象
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > AppConstant.PAGE_SIZE_MAX, ErrorCode.PARAMS_ERROR,
                "每页最多查询 " + AppConstant.PAGE_SIZE_MAX + " 个应用");
        // 强制限定为当前登录用户的应用，防止越权查询他人应用
        appQueryRequest.setUserId(String.valueOf(loginUser.getId()));
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 调用方未指定排序字段时，默认按创建时间倒序
        if (StrUtil.isBlank(appQueryRequest.getSortField())) {
            queryWrapper.orderBy("createTime", false);
        }
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(appService.getAppVOPage(appPage));
    }

    /**
     * 分页查询精选应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param appQueryRequest 查询请求参数
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > AppConstant.PAGE_SIZE_MAX, ErrorCode.PARAMS_ERROR,
                "每页最多查询 " + AppConstant.PAGE_SIZE_MAX + " 个应用");
        QueryWrapper queryWrapper = appService.getGoodAppQueryWrapper(appQueryRequest);
        // 调用方未指定排序字段时，精选应用按优先级降序、创建时间降序排列
        if (StrUtil.isBlank(appQueryRequest.getSortField())) {
            queryWrapper.orderBy("priority", false).orderBy("createTime", false);
        }
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(appService.getAppVOPage(appPage));
    }

    // endregion

    // region 管理员接口

    /**
     * 删除应用（管理员，可删除任意应用）
     *
     * @param deleteRequest 删除请求
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || StrUtil.isBlank(deleteRequest.getId()), ErrorCode.PARAMS_ERROR);
        long id = IdUtils.parseId(deleteRequest.getId(), "id");
        // 校验应用是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新应用（管理员，支持更新应用名称、应用封面、优先级）
     *
     * @param appAdminUpdateRequest 应用更新请求
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null || StrUtil.isBlank(appAdminUpdateRequest.getId()),
                ErrorCode.PARAMS_ERROR);
        long id = IdUtils.parseId(appAdminUpdateRequest.getId(), "id");
        // 校验应用是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        // 忽略 null 字段，避免只改名称时把 cover / priority 清空
        BeanUtil.copyProperties(appAdminUpdateRequest, app, CopyOptions.create().setIgnoreNullValue(true));
        app.setId(id);
        appService.validApp(app, false);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询应用列表（管理员，支持根据除时间外的任何字段查询，每页数量不限）
     *
     * @param appQueryRequest 查询请求参数
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appService.getAppVOPage(appPage));
    }

    /**
     * 根据 id 获取应用详情（管理员，返回完整字段）
     *
     * @param id 应用 id
     */
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppById(@RequestParam("id") String id) {
        long appId = IdUtils.parseId(id, "id");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(app);
    }

    // endregion
}
