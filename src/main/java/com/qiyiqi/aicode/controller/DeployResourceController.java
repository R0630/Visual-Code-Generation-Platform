package com.qiyiqi.aicode.controller;

import com.qiyiqi.aicode.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

/**
 * 部署站点静态资源控制器
 * <p>
 * 部署产物保存在 code_deploy/{deployKey} 目录（见 AppServiceImpl.DeployApp），
 * 本控制器提供其对外访问：
 * <ul>
 *   <li>内部访问：http://localhost:8123/api/deploy/{deployKey}/ （兼容旧链接）</li>
 *   <li>外部访问：http://localhost:8123/api/xinggou/{deployKey}/ （对外展示用，简短专业）</li>
 * </ul>
 * 生产环境可通过 Nginx 把 /xinggou/xxx rewrite 到 /api/xinggou/xxx，去掉 /api 前缀。
 * <p>
 * 注意与 StaticResourceController（生成产物预览，code_output/{codeGenType}_{appId}）区分。
 */
@RestController
@RequestMapping({"/deploy", "/xinggou"})
public class DeployResourceController {

    /** 部署产物根目录 */
    private static final String DEPLOY_ROOT_DIR = AppConstant.CODE_DEPLOY_ROOT_DIR;

    @GetMapping("/{deployKey}/**")
    public ResponseEntity<?> serveDeployResource(@PathVariable String deployKey, HttpServletRequest request) {
        try {
            // 获取资源路径并截取掉 /deploy/{deployKey} 或 /xinggou/{deployKey} 前缀
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String prefix = request.getServletPath().contains("/xinggou")
                    ? "/xinggou/" + deployKey
                    : "/deploy/" + deployKey;
            resourcePath = resourcePath.substring(prefix.length());

            // 目录访问（不带斜杠）重定向到带斜杠的 URL，保证相对路径资源正确加载
            if (resourcePath.isEmpty()) {
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .header(HttpHeaders.LOCATION, request.getRequestURI() + "/")
                        .build();
            }

            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }

            // 构建文件路径并做路径穿越校验
            File rootDir = new File(DEPLOY_ROOT_DIR);
            File file = new File(rootDir, deployKey + resourcePath);
            if (!file.getCanonicalPath().startsWith(rootDir.getCanonicalPath())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 检查文件存在
            if (!file.exists() || file.isDirectory()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, getContentTypeWithCharset(file.getName()))
                    .body(new FileSystemResource(file));
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
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
