package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.syty.dto.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 打印素材管理 Controller (V1.4 新增)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/print-assets")
@Tag(name = "打印素材管理", description = "素材上传、列表查询（含 MD5 指纹）")
public class PrintAssetController {

    /** 素材存储根目录 */
    @Value("${print.assets.base-path:assets/print/}")
    private String basePath;

    /** 素材对外访问 URL 前缀 */
    @Value("${print.assets.url-prefix:/assets/print/}")
    private String urlPrefix;

    @PostConstruct
    public void init() {
        Path path = Paths.get(basePath);
        try {
            Files.createDirectories(path);
            log.info("【素材目录初始化】目录已就绪: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("【素材目录初始化】创建目录失败: {}", basePath, e);
        }
    }

    // ==================== API 1: 上传素材 ====================

    @PostMapping("/upload")
    @SaCheckPermission("print:asset:upload")
    @Operation(summary = "上传打印素材", description = "接收文件上传，保存到 assets/print/ 目录，返回访问 URL")
    public ApiResult<AssetUploadResponse> upload(
            @Parameter(description = "素材文件", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "素材标识（可选，默认使用文件名）")
            @RequestParam(required = false) String key) {

        try {
            // 1. 参数校验
            if (file.isEmpty()) {
                return ApiResult.error(400, "上传文件不能为空");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                return ApiResult.error(400, "文件名不能为空");
            }

            // 安全校验：文件名不能包含路径穿越字符
            if (originalFilename.contains("..") || originalFilename.contains("/")
                    || originalFilename.contains("\\")) {
                return ApiResult.error(400, "文件名非法，禁止路径穿越");
            }

            // 2. 确定存储文件名
            String saveFilename = (key != null && !key.isBlank()) ? key : originalFilename;
            Path savePath = Paths.get(basePath).resolve(saveFilename).normalize();

            // 安全检查：确保文件路径在 basePath 内
            if (!savePath.startsWith(Paths.get(basePath).toAbsolutePath())) {
                return ApiResult.error(400, "非法路径，禁止越权访问");
            }

            // 3. 保存文件
            savePath.toFile().getParentFile().mkdirs();
            file.transferTo(savePath.toFile());

            // 4. 计算 MD5
            String md5 = calculateMd5(savePath.toFile());

            // 5. 构建响应
            AssetUploadResponse response = new AssetUploadResponse();
            response.setKey(saveFilename);
            response.setUrl(urlPrefix + saveFilename);
            response.setMd5(md5);
            response.setSize(file.getSize());
            response.setFilename(originalFilename);

            log.info("【素材上传】成功: key={}, url={}, md5={}", saveFilename, response.getUrl(), md5);
            return ApiResult.success(response);
        } catch (IOException e) {
            log.error("【素材上传】文件保存失败", e);
            return ApiResult.error("文件保存失败: " + e.getMessage());
        }
    }

    // ==================== API 2: 列出所有素材 ====================

    @GetMapping("/list")
    @SaCheckPermission("print:asset:view")
    @Operation(summary = "获取素材列表", description = "扫描 assets/print/ 目录，返回文件列表（含 MD5 指纹）")
    public ApiResult<List<AssetItem>> list() {
        try {
            Path dirPath = Paths.get(basePath).toAbsolutePath().normalize();

            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                log.warn("【素材列表】目录不存在: {}", dirPath);
                return ApiResult.success(new ArrayList<>());
            }

            List<AssetItem> assets = new ArrayList<>();
            try (Stream<Path> stream = Files.list(dirPath)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> {
                            AssetItem item = new AssetItem();
                            String filename = path.getFileName().toString();
                            item.setKey(filename);
                            item.setUrl(urlPrefix + filename);
                            try {
                                item.setMd5(calculateMd5(path.toFile()));
                            } catch (Exception e) {
                                log.warn("【素材列表】计算MD5失败: {}", filename, e);
                                item.setMd5("UNKNOWN");
                            }
                            assets.add(item);
                        });
            }

            log.info("【素材列表】查询成功，共 {} 个文件", assets.size());
            return ApiResult.success(assets);
        } catch (IOException e) {
            log.error("【素材列表】扫描目录失败", e);
            return ApiResult.error("扫描目录失败: " + e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 计算文件 MD5
     */
    private String calculateMd5(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(fis);
        }
    }

    // ==================== 内部 DTO ====================

    @Data
    public static class AssetUploadResponse {
        private String key;
        private String url;
        private String md5;
        private long size;
        private String filename;
    }

    @Data
    public static class AssetItem {
        private String key;
        private String url;
        private String md5;
    }
}
