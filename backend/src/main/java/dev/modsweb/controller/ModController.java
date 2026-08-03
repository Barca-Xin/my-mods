package dev.modsweb.controller;

import dev.modsweb.dto.DependencyGraphDto;
import dev.modsweb.dto.ModDetailDto;
import dev.modsweb.dto.ModListItemDto;
import dev.modsweb.dto.PageResult;
import dev.modsweb.dto.VersionDto;
import dev.modsweb.service.ModService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mods")
public class ModController {

    private final ModService modService;

    public ModController(ModService modService) {
        this.modService = modService;
    }

    /** 列表：双下拉（游戏版本 × 加载器）联动筛选，后端已聚合出每个模组的最新版（切入点三） */
    @GetMapping
    public PageResult<ModListItemDto> list(@RequestParam(required = false) String gameVersion,
                                           @RequestParam(required = false) String loader,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "12") int size) {
        return modService.listMods(gameVersion, loader, category, keyword, page, Math.min(size, 100));
    }

    @GetMapping("/{slug}")
    public ModDetailDto detail(@PathVariable String slug) {
        return modService.getDetail(slug);
    }

    /** 详情页版本切换器数据（切入点四）：可按游戏版本过滤 */
    @GetMapping("/{slug}/versions")
    public List<VersionDto> versions(@PathVariable String slug,
                                     @RequestParam(required = false) String gameVersion) {
        return modService.getVersions(slug, gameVersion);
    }

    /** 依赖图谱（切入点六） */
    @GetMapping("/{slug}/dependencies")
    public DependencyGraphDto dependencies(@PathVariable String slug) {
        return modService.getDependencies(slug);
    }
}
