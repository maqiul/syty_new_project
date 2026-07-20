package com.syty.controller;
import com.syty.dto.ApiResult;
import com.syty.entity.PackageInfo;
import com.syty.mapper.PackageInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/platform/packages")
@RequiredArgsConstructor
public class PackageController {
    private final PackageInfoMapper packageMapper;

    @GetMapping("/list")
    public ApiResult<List<PackageInfo>> list() {
        return ApiResult.success(packageMapper.selectList(null));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody PackageInfo pkg) {
        packageMapper.insert(pkg);
        return ApiResult.success();
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody PackageInfo pkg) {
        pkg.setId(id);
        packageMapper.updateById(pkg);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        packageMapper.deleteById(id);
        return ApiResult.success();
    }
}
