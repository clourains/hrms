package com.hrms.controller;

import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 附件上传与读取（简历图片 / PDF）
 *
 * 设计要点：
 * 1. 文件落在本地目录（hrms.upload.dir，默认 hrms-backend/uploads），文件名用 UUID，
 *    库里只存相对路径，原始文件名单独存一列用于下载时还原。
 * 2. 读取走鉴权接口而不是静态资源映射：静态映射必须在 SecurityConfig 里 open，
 *    等于把简历变成「知道 URL 就能看」，因此这里用 /file/view 与 /file/download 输出流，
 *    Spring Security 仍然要求登录。
 * 3. 所有 path 参数都做目录穿越校验（normalize 后必须仍在根目录内）。
 */
@RestController
@RequestMapping("/file")
public class FileController {

    /** 单文件上限，与 application.yml 的 multipart 配置保持一致 */
    private static final long MAX_SIZE = 10L * 1024 * 1024;

    private static final Set<String> IMAGE_EXT =
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp"));

    private static final Set<String> ALLOWED_EXT;

    static {
        ALLOWED_EXT = new HashSet<>(IMAGE_EXT);
        ALLOWED_EXT.add("pdf");
    }

    @Value("${hrms.upload.dir:./uploads}")
    private String uploadDir;

    /** 上传简历，返回相对路径 + 原始文件名 + 大小 + 归类，前端随候选人一起提交 */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.trim().isEmpty()) {
            return Result.error("文件名不能为空");
        }
        String ext = extOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error("只支持 jpg / png / gif / webp / bmp 图片或 pdf 文件");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.error("文件不能超过 10MB");
        }

        String sub = "resume/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = baseDir().resolve(sub);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName).normalize();
            if (!target.startsWith(baseDir())) {
                return Result.error("非法路径");
            }
            file.transferTo(target.toFile());

            Map<String, Object> data = new HashMap<>();
            data.put("path", sub + "/" + storedName);
            data.put("name", original);
            data.put("size", file.getSize());
            data.put("type", IMAGE_EXT.contains(ext) ? "image" : "pdf");
            return Result.success("上传成功", data);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /** 在线查看（图片直接内联渲染，pdf 交给浏览器内置阅读器） */
    @GetMapping("/view")
    public void view(@RequestParam("path") String path, HttpServletResponse response) {
        writeFile(path, null, false, response);
    }

    /** 下载：带上原始文件名 */
    @GetMapping("/download")
    public void download(@RequestParam("path") String path,
                         @RequestParam(value = "name", required = false) String name,
                         HttpServletResponse response) {
        writeFile(path, name, true, response);
    }

    private void writeFile(String path, String originalName, boolean attachment, HttpServletResponse response) {
        try {
            Path base = baseDir();
            Path file = base.resolve(path == null ? "" : path).normalize();
            if (!file.startsWith(base) || !Files.isRegularFile(file)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String ext = extOf(file.getFileName().toString());
            response.setContentType(mimeOf(ext));
            response.setContentLengthLong(Files.size(file));
            if (attachment) {
                String name = (originalName == null || originalName.trim().isEmpty())
                        ? file.getFileName().toString() : originalName;
                String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
            }
            try (InputStream in = Files.newInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Path baseDir() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    private String mimeOf(String ext) {
        switch (ext) {
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png":  return "image/png";
            case "gif":  return "image/gif";
            case "webp": return "image/webp";
            case "bmp":  return "image/bmp";
            case "pdf":  return "application/pdf";
            default:     return "application/octet-stream";
        }
    }
}
