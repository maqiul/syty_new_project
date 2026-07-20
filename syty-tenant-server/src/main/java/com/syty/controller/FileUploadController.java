package com.syty.controller;
import com.syty.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;
/**
 * 文件上传控制器
 * 用于处理图片上传（如球员照片、穿线现场记录）
 */
@Tag(name = "文件管理", description = "图片上传与访")
@RestController
@RequestMapping("/api/file")
public class FileUploadController {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    @Value("${file.upload-path:./uploads}")
    private String uploadPath;
    @PostMapping("/upload")
    @Operation(summary = "上传图片")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return Result.error("文件为空");
        if (file.getSize() > MAX_FILE_SIZE) return Result.error("文件大小不能超过 10MB");
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            return Result.error("只支持JPG/PNG/GIF/WEBP 格式的图片");
        }
        String newName = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadPath);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
        return Result.success("/api/file/view/" + newName);
    }
    @GetMapping("/view/{filename}")
    @Operation(summary = "预览图片", hidden = true)
    public void view(@PathVariable String filename, HttpServletResponse response) throws IOException {
        Path path = Paths.get(uploadPath, filename);
        if (Files.exists(path)) {
            String contentType = Files.probeContentType(path);
            response.setContentType(contentType != null ? contentType : "image/jpeg");
            Files.copy(path, response.getOutputStream());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
