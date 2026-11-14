package com.qiyiqi.aicode.controller;

import com.qiyiqi.aicode.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于浏览）
    private static final String PREVIEW_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 提供静态资源访问，支持目录重定向与 Vue 项目构建产物优先
     * 访问格式：http://localhost:8123/api/static/{dirName}[/{fileName}]
     *
     * 对于 vue_project 类型目录：
     * - 如果有 dist/index.html（构建产物），优先返回 dist 下的内容
     * - 否则回退到根目录的源码文件
     */
    @GetMapping("/{dirName}/**")
    public ResponseEntity<?> serveStaticResource(@PathVariable String dirName,
                                                 HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

            // 截取掉 /static/{dirName} 前缀
            resourcePath = resourcePath.substring(("/static/" + dirName).length());

            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .header(HttpHeaders.LOCATION, request.getRequestURI() + "/")
                        .build();
            }

            File rootDir = new File(PREVIEW_ROOT_DIR);

            // ===== 路径选择逻辑 =====
            File actualRootDir = rootDir;
            String actualPath = resourcePath;

            // Vue 项目目录：如果有 dist，优先从 dist 提供文件
            boolean isVueProject = dirName.startsWith("vue_project");
            if (isVueProject) {
                File distIndex = new File(rootDir, dirName + "/dist/index.html");
                if (distIndex.exists()) {
                    // dist 目录有 index.html，重写查找路径
                    if (resourcePath.equals("/") || resourcePath.equals("/index.html")) {
                        // 根目录或 index.html → 指向 dist/index.html
                        actualRootDir = new File(rootDir, dirName + "/dist");
                        actualPath = "/index.html";
                    } else {
                        // 其他资源（css/js 等）也优先从 dist 找
                        // 先尝试 dist 下的路径，找不到再回退到根目录
                        File distPath = new File(rootDir, dirName + "/dist" + resourcePath);
                        if (distPath.exists() && !distPath.isDirectory()) {
                            actualRootDir = new File(rootDir, dirName + "/dist");
                        } else {
                            // 回退到根目录（比如访问 vite.config.js 等源码文件）
                            actualRootDir = new File(rootDir, dirName);
                        }
                        actualPath = resourcePath;
                    }
                } else {
                    // 没有 dist，使用根目录
                    actualRootDir = new File(rootDir, dirName);
                }
            } else {
                actualRootDir = new File(rootDir, dirName);
            }

            // 默认返回 index.html
            if (actualPath.equals("/")) {
                actualPath = "/index.html";
            }

            // 构建文件路径
            File file = new File(actualRootDir, actualPath.substring(1));

            // 防止路径穿越攻击
            if (!file.getCanonicalPath().startsWith(actualRootDir.getCanonicalPath())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 检查文件是否存在
            if (!file.exists() || file.isDirectory()) {
                return ResponseEntity.notFound().build();
            }

            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, getContentTypeWithCharset(file.getName()))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=UTF-8";
        if (fileName.endsWith(".css")) return "text/css; charset=UTF-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (fileName.endsWith(".map")) return "application/json; charset=UTF-8";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".json")) return "application/json; charset=UTF-8";
        if (fileName.endsWith(".woff") || fileName.endsWith(".woff2")) return "font/woff";
        return "application/octet-stream";
    }
}