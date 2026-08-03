package dev.modsweb.config;

import dev.modsweb.entity.FileRecord;
import dev.modsweb.entity.Mod;
import dev.modsweb.entity.ModDependency;
import dev.modsweb.entity.ModVersion;
import dev.modsweb.repository.FileRecordRepository;
import dev.modsweb.repository.ModDependencyRepository;
import dev.modsweb.repository.ModRepository;
import dev.modsweb.repository.ModVersionRepository;
import dev.modsweb.service.StorageService;
import dev.modsweb.util.Md5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 首跑种子数据：注入用户现有的 3 个真实 Mod（AdvancedEnchanting / ChainMiner / InventorySorter）。
 * 每个版本生成一个最小 stub jar（含 fabric.mod.json），走完整「存储 → FileRecord → 下载」链路。
 * 用户可在后台上传真实 jar 覆盖。
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ModRepository modRepository;
    private final ModVersionRepository versionRepository;
    private final ModDependencyRepository dependencyRepository;
    private final FileRecordRepository fileRecordRepository;
    private final StorageService storageService;

    public DataSeeder(ModRepository modRepository, ModVersionRepository versionRepository,
                      ModDependencyRepository dependencyRepository, FileRecordRepository fileRecordRepository,
                      StorageService storageService) {
        this.modRepository = modRepository;
        this.versionRepository = versionRepository;
        this.dependencyRepository = dependencyRepository;
        this.fileRecordRepository = fileRecordRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (modRepository.count() > 0) {
            return;
        }
        log.info("Seeding initial mod data...");

        seedMod("advancedenchanting", "Advanced Enchanting", "魔法", "Fabric",
                "自定义附魔台：自定义配方、附魔选择、经验与钻石消耗。现支持 1.20.1 与 1.21.4。",
                "https://raw.githubusercontent.com/yourname/advancedenchanting/main/icon.png",
                "https://github.com/yourname/advancedenchanting",
                "https://github.com/yourname/advancedenchanting/wiki",
                List.of("fabric-api"),
                new SeedVersion("1.0.0", "1.20.1",
                        "首个发布版本：自定义附魔台、钻石消耗、≥35 级门槛（创造豁免）。", true, 160),
                new SeedVersion("1.0.0", "1.21.4",
                        "迁移到 1.21.4：物品模型系统重构适配、新网络包格式。", true, 120));

        seedMod("chainminer", "Chain Miner", "实用", "Fabric",
                "连锁采集：按住鼠标中键挖整条矿脉，支持配置最大方块数。",
                "https://raw.githubusercontent.com/yourname/chainminer/main/icon.png",
                "https://github.com/yourname/chainminer",
                null,
                List.of("fabric-api"),
                new SeedVersion("1.0.0", "1.21.4",
                        "支持连锁整条矿脉，含最大方块数配置。", true, 45));

        seedMod("inventorysorter", "Inventory Sorter", "实用", "Fabric",
                "一键整理：按 J 键整理容器与背包，自动堆叠同类物品。",
                "https://raw.githubusercontent.com/yourname/inventorysorter/main/icon.png",
                "https://github.com/yourname/inventorysorter",
                null,
                List.of("fabric-api"),
                new SeedVersion("1.0.0", "1.21.4",
                        "一键整理背包与所有容器。", true, 30));

        log.info("Seed complete.");
    }

    @Transactional
    protected void seedMod(String slug, String name, String category, String loader, String shortDesc,
                           String logoUrl, String sourceUrl, String wikiUrl,
                           List<String> externalDeps, SeedVersion... versions) throws Exception {
        Mod mod = new Mod();
        mod.setSlug(slug);
        mod.setName(name);
        mod.setCategory(category);
        mod.setModLoader(loader);
        mod.setShortDesc(shortDesc);
        mod.setLogoUrl(logoUrl);
        mod.setSourceCodeUrl(sourceUrl);
        mod.setWikiUrl(wikiUrl);
        modRepository.save(mod);

        for (SeedVersion sv : versions) {
            seedVersion(mod, sv);
        }

        for (String depName : externalDeps) {
            ModDependency dep = new ModDependency();
            dep.setMod(mod);
            dep.setName(depName);
            dep.setType("depends");
            dependencyRepository.save(dep);
        }
    }

    @Transactional
    protected void seedVersion(Mod mod, SeedVersion sv) throws Exception {
        String bucketPath = "mods/" + mod.getSlug() + "/" + mod.getSlug() + "-" + sv.modVersion + "-" + sv.gameVersion + ".jar";
        byte[] bytes = stubJar(mod.getSlug(), sv.modVersion, mod.getName());
        storageService.save(bucketPath, bytes);

        FileRecord fr = new FileRecord();
        fr.setFileName(bucketPath);
        fr.setBucketPath(bucketPath);
        fr.setSize((long) bytes.length);
        fr.setMd5(Md5Util.md5Hex(bytes));
        fr = fileRecordRepository.save(fr);

        ModVersion version = new ModVersion();
        version.setMod(mod);
        version.setModVersion(sv.modVersion);
        version.setGameVersion(sv.gameVersion);
        version.setModLoader(mod.getModLoader());
        version.setChangelog(sv.changelog);
        version.setRecommended(sv.recommended);
        version.setFileRecord(fr);
        version.setFileSize((long) bytes.length);
        version.setMd5(fr.getMd5());
        version.setDownloadCount(0L);
        version.setReleaseDate(LocalDateTime.now().minusDays(sv.daysAgo));
        versionRepository.save(version);
    }

    /** 生成带 fabric.mod.json 的最小 stub jar，保证下载链路可测 */
    private byte[] stubJar(String modId, String version, String name) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry("fabric.mod.json"));
            String json = "{\"schemaVersion\":1,\"id\":\"" + modId + "\",\"version\":\"" + version
                    + "\",\"name\":\"" + name + "\",\"depends\":{\"fabric-api\":\"*\"}}";
            zos.write(json.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    private record SeedVersion(String modVersion, String gameVersion, String changelog,
                               boolean recommended, long daysAgo) {
    }
}
