package dev.modsweb.controller;

import dev.modsweb.service.AdminService;
import dev.modsweb.service.JarMetaParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminToolController {

    private final AdminService adminService;

    public AdminToolController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 上传 jar 自动解析元数据，回填发布表单（切入点五） */
    @PostMapping(value = "/parse-jar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JarMetaParser.JarMeta parseJar(@RequestParam("file") MultipartFile file) {
        return adminService.parseJar(file);
    }
}
