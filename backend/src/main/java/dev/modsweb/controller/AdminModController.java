package dev.modsweb.controller;

import dev.modsweb.dto.ModDetailDto;
import dev.modsweb.dto.VersionDto;
import dev.modsweb.dto.admin.ModRequest;
import dev.modsweb.dto.admin.VersionForm;
import dev.modsweb.entity.Mod;
import dev.modsweb.entity.ModVersion;
import dev.modsweb.service.AdminService;
import dev.modsweb.service.ModService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/mods")
public class AdminModController {

    private final AdminService adminService;
    private final ModService modService;

    public AdminModController(AdminService adminService, ModService modService) {
        this.adminService = adminService;
        this.modService = modService;
    }

    @GetMapping
    public List<ModDetailDto> list() {
        return modService.listAllForAdmin();
    }

    @PostMapping
    public ModDetailDto create(@Valid @RequestBody ModRequest request) {
        Mod mod = adminService.createMod(request);
        return modService.getDetail(mod.getSlug());
    }

    @PutMapping("/{id}")
    public ModDetailDto update(@PathVariable Long id, @Valid @RequestBody ModRequest request) {
        Mod mod = adminService.updateMod(id, request);
        return modService.getDetail(mod.getSlug());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminService.deleteMod(id);
    }

    /**
     * 版本上传（切入点五）：multipart 携带 jar 文件 + 普通表单字段。
     * 依赖列表来自 jar 自动解析或手动填写，可重复传参。
     */
    @PostMapping(value = "/{modId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VersionDto addVersion(@PathVariable Long modId,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String modVersion,
                                 @RequestParam(required = false) String gameVersion,
                                 @RequestParam(required = false) String modLoader,
                                 @RequestParam(required = false) String changelog,
                                 @RequestParam(required = false) String releaseDate,
                                 @RequestParam(required = false) Boolean recommended,
                                 @RequestParam(required = false) List<String> dependencies) {
        VersionForm form = new VersionForm(modVersion, gameVersion, modLoader, changelog,
                releaseDate, recommended, dependencies);
        ModVersion version = adminService.addVersion(modId, file, form);
        return modService.toVersionDto(version);
    }

    @DeleteMapping("/{modId}/versions/{versionId}")
    public void deleteVersion(@PathVariable Long modId, @PathVariable Long versionId) {
        adminService.deleteVersion(modId, versionId);
    }

    @PutMapping("/{modId}/versions/{versionId}/recommended")
    public VersionDto setRecommended(@PathVariable Long modId, @PathVariable Long versionId,
                                     @RequestBody Map<String, Boolean> body) {
        boolean recommended = body.getOrDefault("recommended", false);
        ModVersion version = adminService.setRecommended(modId, versionId, recommended);
        return modService.toVersionDto(version);
    }
}
