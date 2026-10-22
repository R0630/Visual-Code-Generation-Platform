package com.qiyiqi.aicode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.qiyiqi.aicode.model.dto.app.AppQueryRequest;
import com.qiyiqi.aicode.model.entity.App;
import com.qiyiqi.aicode.model.vo.AppVO;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author qiyiqi
 */
public interface AppService extends IService<App> {

    /**
     * 校验应用参数
     *
     * @param app 应用
     * @param add true 为创建时校验，false 为更新时校验
     */
    void validApp(App app, boolean add);

    /**
     * 根据查询条件构造数据查询参数（管理员视角，支持除时间外的任何字段）
     *
     * @param appQueryRequest 应用查询请求
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 构造精选应用的查询条件（在通用查询条件基础上追加优先级筛选）
     *
     * @param appQueryRequest 应用查询请求
     */
    QueryWrapper getGoodAppQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取脱敏后的应用信息（含创建者信息）
     *
     * @param app 应用
     */
    AppVO getAppVO(App app);

    /**
     * 获取脱敏后的应用信息（分页，含创建者信息）
     *
     * @param appPage 应用分页
     */
    Page<AppVO> getAppVOPage(Page<App> appPage);

    /**
     * 获取脱敏后的应用信息（列表，含创建者信息）
     *
     * @param appList 应用列表
     */
    List<AppVO> getAppVOList(List<App> appList);
}
