package com.qiyiqi.aicode.ai.tools;

import com.qiyiqi.aicode.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 文件写入工具
 *
 * 优化策略：
 * 1. 第一次写入文件时，自动从 classpath 的 templates/vue-project 拷贝完整框架模板到项目目录
 *    （package.json / vite.config.js / index.html / main.js / App.vue / router / global.css 等）
 * 2. AI 只需写入业务相关的页面和组件代码，节省大量重复 token
 * 3. 通过 FIRST_WRITE_TRIGGERED 标记确保模板只拷贝一次
 * 4. 写入成功后通过 ACTIVE_SINKS 桥接推送给正在处理该 appId 的 SSE 流
 */
@Slf4j
public class FileWriteTool {

    /** 活跃的工具事件 Sink 注册表：appId → 对应的 Sinks.Many<String> */
    public static final ConcurrentMap<Long, Sinks.Many<String>> ACTIVE_SINKS = new ConcurrentHashMap<>();

    /** 标记某个 appId 是否已触发过模板初始化 */
    private static final ConcurrentMap<Long, Boolean> FIRST_WRITE_TRIGGERED = new ConcurrentHashMap<>();

    /** classpath 下的模板目录 */
    private static final String TEMPLATE_CLASS_PATH = "templates/vue-project";

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            boolean isRelative = !path.isAbsolute();

            // 计算项目根目录
            String projectDirName = "vue_project_" + appId;
            Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);

            // 第一次写入时，自动初始化框架模板
            if (isRelative && FIRST_WRITE_TRIGGERED.putIfAbsent(appId, Boolean.TRUE) == null) {
                initProjectTemplate(projectRoot);
            }

            if (isRelative) {
                path = projectRoot.resolve(relativeFilePath);
            }

            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());

            // 通知前端实时显示工具执行状态
            notifyFrontend(appId, relativeFilePath);

            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 从 classpath 的 templates/vue-project 拷贝完整框架模板到项目目录。
     *
     * 支持两种运行方式：
     * - IDE 开发：classpath 目录直接可访问，用 FileSystems.getDefault() 遍历
     * - jar 包部署：需要解析 jar 内的资源
     */
    private void initProjectTemplate(Path projectRoot) {
        try {
            Files.createDirectories(projectRoot);
            java.net.URL templateUrl = getClass().getClassLoader().getResource(TEMPLATE_CLASS_PATH);
            if (templateUrl == null) {
                log.warn("未找到 classpath 模板目录: {}", TEMPLATE_CLASS_PATH);
                return;
            }

            String protocol = templateUrl.getProtocol();
            if ("file".equals(protocol)) {
                // IDE 开发环境：直接遍历文件系统
                Path templateDir = Paths.get(templateUrl.toURI());
                Files.walk(templateDir).forEach(source -> {
                    try {
                        Path relative = templateDir.relativize(source);
                        Path target = projectRoot.resolve(relative.toString());
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        log.warn("拷贝模板文件失败: {}", source, e);
                    }
                });
            } else if ("jar".equals(protocol)) {
                // jar 包部署：从 jar 中提取模板
                String jarPath = templateUrl.toString().substring(5, templateUrl.toString().indexOf("!"));
                try (JarFile jar = new JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"))) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!name.startsWith(TEMPLATE_CLASS_PATH + "/")) continue;
                        // 去掉模板前缀
                        String relativePath = name.substring(TEMPLATE_CLASS_PATH.length() + 1);
                        if (relativePath.isEmpty()) continue;
                        Path target = projectRoot.resolve(relativePath);
                        if (entry.isDirectory()) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            try (InputStream in = jar.getInputStream(entry)) {
                                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }

            log.info("✅ Vue 项目框架模板初始化完成: {}", projectRoot);
            // 框架文件列表（给日志参考）
            String[] frameworkFiles = {
                    "package.json", "vite.config.js", "index.html",
                    "src/main.js", "src/App.vue",
                    "src/router/index.js", "src/styles/global.css"
            };
            for (String file : frameworkFiles) {
                Path p = projectRoot.resolve(file);
                if (Files.exists(p)) {
                    log.info("  ✔️ {}", file);
                }
            }
        } catch (Exception e) {
            log.error("初始化 Vue 项目框架模板失败", e);
            // 模板拷贝失败不影响后续 AI 写入，让 AI 自己生成全部文件
        }
    }

    /**
     * 通过 Sink 桥接推送工具执行提示到 SSE 流
     */
    private void notifyFrontend(long appId, String relativeFilePath) {
        Sinks.Many<String> sink = ACTIVE_SINKS.get(appId);
        if (sink != null) {
            String hint = "\n\n[工具执行] ✍️ 已写入 " + relativeFilePath + "\n";
            Sinks.EmitResult result = sink.tryEmitNext(hint);
            if (result.isFailure()) {
                log.debug("推送工具执行提示失败 (appId={}, 原因={})", appId, result);
            }
        }
    }
}
