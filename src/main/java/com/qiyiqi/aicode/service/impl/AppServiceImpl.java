package com.qiyiqi.aicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.qiyiqi.aicode.common.IdUtils;
import com.qiyiqi.aicode.common.QueryUtils;
import com.qiyiqi.aicode.constant.AppConstant;
import com.qiyiqi.aicode.core.AiCodeGeneratorFacade;
import com.qiyiqi.aicode.core.builder.VueProjectBuilder;
import com.qiyiqi.aicode.core.handler.StreamHandlerExecutor;
import com.qiyiqi.aicode.exception.BusinessException;
import com.qiyiqi.aicode.exception.ErrorCode;
import com.qiyiqi.aicode.exception.ThrowUtils;
import com.qiyiqi.aicode.mapper.AppMapper;
import com.qiyiqi.aicode.model.dto.app.AppQueryRequest;
import com.qiyiqi.aicode.model.entity.App;
import com.qiyiqi.aicode.model.entity.User;
import com.qiyiqi.aicode.model.enums.ChatHistoryMessageTypeEnum;
import com.qiyiqi.aicode.model.enums.CodeGenTypeEnum;
import com.qiyiqi.aicode.model.vo.AppVO;
import com.qiyiqi.aicode.model.vo.UserVO;
import com.qiyiqi.aicode.service.AppService;
import com.qiyiqi.aicode.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
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
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryServiceImpl chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Override
    public Flux<String> chatToGenCode(Long appId, String userMessage, User LoginUser) {
        //1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户输入提示词不能为空");
        //2.查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        //3.权限校验，仅本人可以和自己应用对话
        if (!app.getUserId().equals(LoginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");

        }
        //4.获取应用代码生成类型（历史数据可能为 null，默认回退为多文件模式）
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            codeGenTypeEnum = CodeGenTypeEnum.MULTI_FILE;
        }
        //5.调用AI前，先保存用户消息到数据库
        chatHistoryService.addChatMessage(appId, userMessage,
                ChatHistoryMessageTypeEnum.USER.getValue(),
                LoginUser.getId());
        //6.调用AI生成代码(流式)
        Flux<String> codeFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId);
        //7.收集AI响应内容，完成后保存记录到对话历史
        return streamHandlerExecutor.doExecute(codeFlux, chatHistoryService, appId, LoginUser, codeGenTypeEnum);

    }


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
        // id 以字符串接收，需转换回 long 参与查询（为空或 0 表示该条件不生效）
        Long id = IdUtils.parseIdOrNull(appQueryRequest.getId(), "id");
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = IdUtils.parseIdOrNull(appQueryRequest.getUserId(), "userId");
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                // 空字符串必须转成 null：MyBatis-Flex 默认只忽略 null，
                // 传空串会生成 cover = '' 从而匹配不到库中为 NULL 的记录
                .eq("cover", QueryUtils.blankToNull(cover))
                .eq("codeGenType", QueryUtils.blankToNull(codeGenType))
                .eq("deployKey", QueryUtils.blankToNull(deployKey))
                // 0 视为「未指定优先级」，不作为查询条件
                .eq("priority", (priority == null || priority == 0) ? null : priority)
                .eq("userId", userId)
                .like("appName", QueryUtils.blankToNull(appName))
                .like("initPrompt", QueryUtils.blankToNull(initPrompt));
        // 排序字段为空时不能调用 orderBy，否则会生成非法的 "ORDER BY  ASC"
        if (StrUtil.isNotBlank(sortField)) {
            ThrowUtils.throwIf(!AppConstant.ALLOWED_SORT_FIELDS.contains(sortField),
                    ErrorCode.PARAMS_ERROR, "不支持按 " + sortField + " 排序");
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        }
        return queryWrapper;
    }

    /**
     * 删除应用时，关联删除对话历史
     * @param id 应用id
     * @return 是否删除成功
     */
    @Override
    public boolean removeById( Serializable id) {
        if( id == null){
            return false;
        }
        Long  appId = Long.valueOf(id.toString());
        if( appId <= 0 ){
            return false;
        }
        //先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
          log.error("删除应用时，关联删除对话历史失败:{}",e.getMessage());
        }
        //删除应用
        return super.removeById(id);
    }

    @Override
    public QueryWrapper getGoodAppQueryWrapper(AppQueryRequest appQueryRequest) {
        // 精选应用：优先级达到阈值的应用（由管理员设置 priority 决定）
        // 注意：不复用调用方传入的 priority 等值条件，否则传 priority=0 会与 >= 1 冲突导致结果恒为空
        AppQueryRequest goodQueryRequest = new AppQueryRequest();
        BeanUtil.copyProperties(appQueryRequest, goodQueryRequest);
        goodQueryRequest.setPriority(null);
        return getQueryWrapper(goodQueryRequest)
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

    @Override
    public String DeployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以部署自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位 deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取原始代码生成路径（应用访问目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 构建完成后，需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        return String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


}
