package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.Supplier;
import com.syty.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService supplierService;

    @Operation(summary = "分页查询供应商")
    @GetMapping("/page")
    public Result<Page<Supplier>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(Supplier::getSportType, sportType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Supplier::getName, keyword)
                    .or().like(Supplier::getContactPerson, keyword)
                    .or().like(Supplier::getPhone, keyword));
        }
        wrapper.orderByDesc(Supplier::getId);
        return Result.success(supplierService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "供应商列表(下拉)")
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(Supplier::getSportType, sportType);
        }
        return Result.success(supplierService.list(wrapper));
    }

    @Operation(summary = "获取供应商详情")
    @GetMapping("/{id}")
    public Result<Supplier> get(@PathVariable Long id) {
        return Result.success(supplierService.getById(id));
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    public Result<Supplier> add(@RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return Result.success(supplier);
    }

    @Operation(summary = "修改供应商")
    @PutMapping
    public Result<Void> update(@RequestBody Supplier supplier) {
        supplierService.updateById(supplier);
        return Result.success();
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.removeById(id);
        return Result.success();
    }
}
