package com.qiyiqi.aicode.generator;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MyBatisCodeGenerator {

    // 需要生成的表名
    private static final String[] TABLE_NAMES = {"app"};

    public static void main(String[] args) {
        // 从 classpath 读取 application.yml
        Dict dict = YamlUtil.load(
                IoUtil.getReader(ResourceUtil.getStream("application.yml"), StandardCharsets.UTF_8)
        );
        System.out.println("dict = " + dict);

        Map<String, Object> dataSourceConfig = dict.getByPath("spring.datasource");
        if (dataSourceConfig == null) {
            throw new IllegalStateException("application.yml 中未找到 spring.datasource 配置");
        }

        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));

        // 配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 创建配置内容
        GlobalConfig globalConfig = createGlobalConfig();

        // 通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        // 生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();

        globalConfig.getPackageConfig()
                .setBasePackage("com.qiyiqi.aicode.genresult");

        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                .setLogicDeleteColumn("isDelete");

        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        globalConfig.enableController();

        globalConfig.getJavadocConfig()
                .setSince("");

        return globalConfig;
    }
}