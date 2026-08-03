package dev.modsweb.service;

import dev.modsweb.config.AppProperties;
import dev.modsweb.config.JwtService;
import dev.modsweb.dto.admin.LoginRequest;
import dev.modsweb.dto.admin.ModRequest;
import dev.modsweb.dto.admin.VersionForm;
import dev.modsweb.entity.FileRecord;
import dev.modsweb.entity.Mod;
import dev.modsweb.entity.ModDependency;
import dev.modsweb.entity.ModVersion;
import dev.modsweb.exception.ApiException;
import dev.modsweb.repository.FileRecordRepository;
import dev.modsweb.repository.ModDependencyRepository;
import dev.modsweb.repository.ModRepository;
import dev.modsweb.repository.ModVersionRepository;
import dev.modsweb.util.Md5Util;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final AppProperties props;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ModRepository modRepository;
    private final ModVersionRepository versionRepository;
    private final ModDependencyRepository dependencyRepository;
    private final FileRecordRepository fileRecordRepository;
    private final StorageService storageService;
    private final JarMetaParser jarMetaParser;

    public AdminService(AppProperties props, PasswordEncoder passwordEncoder, JwtService jwtService,
                        ModRepository modRepository, ModVersionRepository versionRepository,
                        ModDependencyRepository dependencyRepository, FileRecordRepository fileRecordRepository,
                        StorageService storageService, JarMetaParser jarMetaParser) {
        this.props = props;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.modRepository = modRepository;
        this.versionRepository = versionRepository;
        this.dependencyRepository = dependencyRepository;
        this.fileRecordRepository = fileRecordRepository;
        this.storageService = storageService;
        this.jarMetaParser = jarMetaParser;
    }

    public Map<String, Object> login(LoginRequest req) {
        boolean nameOk = props.getAdmin().getUsername().equals(req.username());
        boolean passOk = passwordEncoder.matches(req.password(), props.getAdmin().getPasswordHash());
        if (!nameOk || !passOk) {
            throw ApiException.unauthorized("用户名或密码错误");
        }
        return Map.of("token", jwtService.createToken(req.username()));
    }

    /** 上传 jar → 解析 mods.toml / fabric.mod.json（切入点五） */
    public JarMetaParser.JarMeta parseJar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("请上传 jar 文件");
        }
        try {
            return jarMetaParser.parse(file.getBytes());
        } catch (IOException e) {
            throw ApiException.badRequest("读取上传文件失败");
        }
    }

    @Transactional
    public Mod createMod(ModRequest req) {
        if (modRepository.existsBySlug(req.slug())) {
            throw ApiException.badRequest("slug 已存在: " + req.slug());
        }
        Mod mod = new Mod();
        applyMod(mod, req);
        return modRepository.save(mod);
    }

    @Transactional
    public Mod updateMod(Long id, ModRequest req) {
        Mod mod = getMod(id);
        if (!mod.getSlug().equals(req.slug()) && modRepository.existsBySlug(req.slug())) {
            throw ApiException.badRequest("slug 已存在: " + req.slug());
        }
        applyMod(mod, req);
        return modRepository.save(mod);
    }

    @Transactional
    public void deleteMod(Long id) {
        Mod mod = getMod(id);
        for (ModVersion v : mod.getVersions()) {
            if (v.getFileRecord() != null) {
                storageService.delete(v.getFileRecord().getBucketPath());
            }
        }
        modRepository.delete(mod);
    }

    /** 版本上传（multipart）：存文件 + 算 MD5 + 建 FileRecord + 可选回填依赖 */
    @Transactional
    public ModVersion addVersion(Long modId, MultipartFile file, VersionForm form) {
        Mod mod = getMod(modId);
        ModVersion version = new ModVersion();
        version.setMod(mod);
        version.setModVersion(required(form.modVersion(), "modVersion（模组版本号）"));
        version.setGameVersion(required(form.gameVersion(), "gameVersion（游戏版本）"));
        version.setModLoader(StringUtils.hasText(form.modLoader()) ? form.modLoader() : mod.getModLoader());
        version.setChangelog(form.changelog());
        version.setReleaseDate(parseReleaseDate(form.releaseDate()));
        version.setRecommended(form.recommended() != null && form.recommended());

        if (file != null && !file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                String bucketPath = "mods/" + mod.getSlug() + "/" + mod.getSlug() + "-"
                        + form.modVersion() + "-" + form.gameVersion() + ".jar";
                storageService.save(bucketPath, bytes);

                FileRecord fr = new FileRecord();
                fr.setFileName(file.getOriginalFilename() == null ? bucketPath : file.getOriginalFilename());
                fr.setBucketPath(bucketPath);
                fr.setSize((long) bytes.length);
                fr.setMd5(Md5Util.md5Hex(bytes));
                fr = fileRecordRepository.save(fr);

                version.setFileRecord(fr);
                version.setFileSize((long) bytes.length);
                version.setMd5(fr.getMd5());
            } catch (IOException e) {
                throw ApiException.badRequest("读取上传文件失败");
            }
        }

        // 依赖列表：来自 jar 自动解析或手动填写，替换该 Mod 的依赖集合（切入点五）
        if (form.dependencies() != null) {
            replaceDependencies(mod, form.dependencies());
        }
        return versionRepository.save(version);
    }

    @Transactional
    public void deleteVersion(Long modId, Long versionId) {
        ModVersion version = getVersion(versionId);
        if (!version.getMod().getId().equals(modId)) {
            throw ApiException.notFound("版本不存在");
        }
        if (version.getFileRecord() != null) {
            storageService.delete(version.getFileRecord().getBucketPath());
            fileRecordRepository.delete(version.getFileRecord());
        }
        versionRepository.delete(version);
    }

    @Transactional
    public ModVersion setRecommended(Long modId, Long versionId, boolean recommended) {
        if (recommended) {
            for (ModVersion v : versionRepository.findByModIdOrderByReleaseDateDesc(modId)) {
                v.setRecommended(false);
                versionRepository.save(v);
            }
        }
        ModVersion version = getVersion(versionId);
        version.setRecommended(recommended);
        return versionRepository.save(version);
    }

    private void replaceDependencies(Mod mod, List<String> depNames) {
        dependencyRepository.deleteByModId(mod.getId());
        for (String name : depNames) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String trimmed = name.trim();
            ModDependency dep = new ModDependency();
            dep.setMod(mod);
            // 站内已存在的模组（slug 匹配）→ 内部关联；否则视为外部依赖
            modRepository.findBySlug(trimmed).ifPresentOrElse(
                    internal -> {
                        dep.setDependsOn(internal);
                        dep.setName(internal.getName());
                    },
                    () -> dep.setName(trimmed));
            dependencyRepository.save(dep);
        }
    }

    private void applyMod(Mod mod, ModRequest req) {
        mod.setSlug(req.slug());
        mod.setName(req.name());
        mod.setLogoUrl(req.logoUrl());
        mod.setShortDesc(req.shortDesc());
        mod.setCategory(req.category());
        mod.setModLoader(req.modLoader());
        mod.setSourceCodeUrl(req.sourceCodeUrl());
        mod.setWikiUrl(req.wikiUrl());
        mod.setAutoSyncSource(req.autoSyncSource());
        mod.setDeclaration(req.declaration());
    }

    private Mod getMod(Long id) {
        return modRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("模组不存在"));
    }

    private ModVersion getVersion(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("版本不存在"));
    }

    private String required(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw ApiException.badRequest(field + " 不能为空");
        }
        return value.trim();
    }

    private LocalDateTime parseReleaseDate(String s) {
        if (!StringUtils.hasText(s)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(s).atStartOfDay();
            } catch (DateTimeParseException e) {
                return LocalDateTime.now();
            }
        }
    }
}
