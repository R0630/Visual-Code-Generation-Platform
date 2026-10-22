-- 接口测试数据初始化脚本
-- 加密规则：userPassword = MD5(明文密码 + 'qiyiqi')，与 UserServiceImpl.getEncryptPassword 保持一致
-- 使用方法：mysql -uroot -p ai_code < sql/test_data.sql
--
-- id 说明：
--   项目实体使用 MyBatis-Flex 雪花算法 @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
--   本脚本的 id 由项目自身的 SnowFlakeIDKeyGenerator 生成（18 位，含时间戳+机器位+序列号），
--   与线上真实数据的形态一致，避免自增 id 被遍历猜测。
--   表本身无需改回自增 —— 应用插入时由 MyBatis-Flex 自动填充，这里的固定值仅供测试。

use ai_code;

-- ============================================================
-- 1. 清理历史脏数据（旧数据 userRole 为「普通用户」「用户」「fff」等非法值，无法用于鉴权）
-- ============================================================
DELETE FROM chat_history;
DELETE FROM app;
DELETE FROM user;

-- ============================================================
-- 2. 测试用户（密码统一走加盐 MD5）
--    账号 / 明文密码 / 角色 / id（雪花）
--    admin001 / admin123 / admin   管理员
--    user001  / user1234 / user    普通用户A（拥有 4 个正常应用）
--    user002  / user1234 / user    普通用户B（用于越权测试）
-- ============================================================
INSERT INTO user
(id, userAccount, userPassword, userName, userAvatar, userProfile, userRole, editTime, createTime, updateTime, isDelete)
VALUES
    (471096573665755136, 'admin001', MD5(CONCAT('admin123', 'qiyiqi')), '平台管理员', NULL, '管理员账号，用于测试 /app/admin/* 与 /user/* 管理员接口', 'admin', NOW(), NOW(), NOW(), 0),
    (471096575091818496, 'user001',  MD5(CONCAT('user1234', 'qiyiqi')), '张三',      NULL, '普通用户A，拥有 4 个应用', 'user', NOW(), NOW(), NOW(), 0),
    (471096582666731520, 'user002',  MD5(CONCAT('user1234', 'qiyiqi')), '李四',      NULL, '普通用户B，用于越权测试', 'user', NOW(), NOW(), NOW(), 0);

-- ============================================================
-- 3. 测试应用
--    精选规则：priority >= 1（见 AppConstant.GOOD_APP_MIN_PRIORITY）
--    个人博客网站      user001  priority=0  未部署  —— 普通应用
--    在线计算器        user001  priority=2  已部署  —— 精选（最高优先级）
--    待办清单          user001  priority=1  已部署  —— 精选
--    待删除的临时应用  user001  priority=0  未部署  —— 专门给删除接口测试用
--    李四的相册        user002  priority=0  未部署  —— 越权测试用（user001 不得修改/删除）
--    已删除的应用      user001  priority=0  未部署  —— isDelete=1，用于验证逻辑删除不可见
-- ============================================================
INSERT INTO app
(id, appName, cover, initPrompt, codeGenType, deployKey, deployedTime, priority, userId, editTime, createTime, updateTime, isDelete)
VALUES
    (471096585778905088, '个人博客网站',     NULL,                                    '创建一个简洁的个人博客网站，包含文章列表和文章详情页', NULL,   NULL,               NULL, 0, 471096575091818496, NOW(), NOW(), NOW(), 0),
    (471096596923170816, '在线计算器',       'https://example.com/cover/calc.png',    '创建一个在线计算器网页，支持加减乘除和清空',           'html', 'deploy_calc_001', NOW(), 2, 471096575091818496, NOW(), NOW(), NOW(), 0),
    (471096601549488128, '待办清单',         'https://example.com/cover/todo.png',    '创建一个待办清单应用，支持新增、完成和删除任务',       'html', 'deploy_todo_001', NOW(), 1, 471096575091818496, NOW(), NOW(), NOW(), 0),
    (471096619605966848, '待删除的临时应用', NULL,                                    '这是一个专门用于测试删除接口的临时应用',               NULL,   NULL,               NULL, 0, 471096575091818496, NOW(), NOW(), NOW(), 0),
    (471096623380840448, '李四的相册',       NULL,                                    '创建一个相册展示网站，支持网格布局和点击放大',         NULL,   NULL,               NULL, 0, 471096582666731520, NOW(), NOW(), NOW(), 0),
    (471096638484529152, '已删除的应用',     NULL,                                    '这条数据已逻辑删除，接口应查不到',                     NULL,   NULL,               NULL, 0, 471096575091818496, NOW(), NOW(), NOW(), 1);

-- ============================================================
-- 4. 校验（可选）：可直接复制 id 到 Knife4j 里测试
-- ============================================================
SELECT id, userAccount, userName, userRole, isDelete FROM user ORDER BY id;
SELECT id, appName, priority, deployKey, userId, isDelete FROM app ORDER BY id;
