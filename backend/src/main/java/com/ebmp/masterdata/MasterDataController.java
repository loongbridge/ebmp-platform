package com.ebmp.masterdata;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.BizException;
import com.ebmp.common.PageResult;
import com.ebmp.system.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
public class MasterDataController {

    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    @GetMapping("/customers")
    public ApiResponse<PageResult<Customer>> customers(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String level) {
        Page<Customer> result = customerRepository.search(blank(keyword), blank(level),
                PageRequest.of(page - 1, size, Sort.by("id")));
        result.getContent().forEach(this::enrichCustomer);
        return ApiResponse.ok(PageResult.of(result));
    }

    @GetMapping("/customers/options")
    public ApiResponse<List<Map<String, Object>>> customerOptions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Customer c : customerRepository.findAllByOrderByIdDesc()) {
            list.add(Map.of("id", c.getId(), "name", c.getName(), "level", c.getLevel() == null ? "" : c.getLevel()));
        }
        return ApiResponse.ok(list);
    }

    @GetMapping("/customers/{id}")
    public ApiResponse<Customer> customer(@PathVariable Long id) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new BizException("客户不存在"));
        enrichCustomer(c);
        return ApiResponse.ok(c);
    }

    @PostMapping("/customers")
    public ApiResponse<Customer> createCustomer(@RequestBody Customer body) {
        if (body.getCode() == null || body.getCode().isBlank()) {
            body.setCode("CUS" + System.currentTimeMillis());
        }
        body.setId(null);
        body.setLastContactAt(LocalDateTime.now());
        return ApiResponse.ok(customerRepository.save(body));
    }

    @PutMapping("/customers/{id}")
    public ApiResponse<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer body) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new BizException("客户不存在"));
        copyCustomer(body, c);
        return ApiResponse.ok(customerRepository.save(c));
    }

    @DeleteMapping("/customers/{id}")
    public ApiResponse<Void> deleteCustomer(@PathVariable Long id) {
        contactRepository.deleteByCustomerId(id);
        customerRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/customers/{id}/contacts")
    public ApiResponse<List<Contact>> contacts(@PathVariable Long id) {
        return ApiResponse.ok(contactRepository.findByCustomerIdOrderByIsPrimaryDescIdAsc(id));
    }

    @PostMapping("/contacts")
    public ApiResponse<Contact> createContact(@RequestBody Contact body) {
        body.setId(null);
        return ApiResponse.ok(contactRepository.save(body));
    }

    @DeleteMapping("/contacts/{id}")
    public ApiResponse<Void> deleteContact(@PathVariable Long id) {
        contactRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/suppliers")
    public ApiResponse<PageResult<Supplier>> suppliers(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String category) {
        return ApiResponse.ok(PageResult.of(supplierRepository.search(blank(keyword), blank(category),
                PageRequest.of(page - 1, size, Sort.by("id")))));
    }

    @GetMapping("/suppliers/options")
    public ApiResponse<List<Map<String, Object>>> supplierOptions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Supplier s : supplierRepository.findAllByOrderByIdDesc()) {
            list.add(Map.of("id", s.getId(), "name", s.getName(), "level", s.getLevel() == null ? "" : s.getLevel()));
        }
        return ApiResponse.ok(list);
    }

    @PostMapping("/suppliers")
    public ApiResponse<Supplier> createSupplier(@RequestBody Supplier body) {
        if (body.getCode() == null || body.getCode().isBlank()) {
            body.setCode("SUP" + System.currentTimeMillis());
        }
        body.setId(null);
        return ApiResponse.ok(supplierRepository.save(body));
    }

    @PutMapping("/suppliers/{id}")
    public ApiResponse<Supplier> updateSupplier(@PathVariable Long id, @RequestBody Supplier body) {
        Supplier s = supplierRepository.findById(id).orElseThrow(() -> new BizException("供应商不存在"));
        copySupplier(body, s);
        return ApiResponse.ok(supplierRepository.save(s));
    }

    @DeleteMapping("/suppliers/{id}")
    public ApiResponse<Void> deleteSupplier(@PathVariable Long id) {
        supplierRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/materials")
    public ApiResponse<PageResult<Material>> materials(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String category) {
        return ApiResponse.ok(PageResult.of(materialRepository.search(blank(keyword), blank(category),
                PageRequest.of(page - 1, size, Sort.by("id")))));
    }

    @GetMapping("/materials/options")
    public ApiResponse<List<Material>> materialOptions() {
        return ApiResponse.ok(materialRepository.findAllByOrderByIdAsc());
    }

    @PostMapping("/materials")
    public ApiResponse<Material> createMaterial(@RequestBody Material body) {
        if (body.getCode() == null || body.getCode().isBlank()) {
            body.setCode("MAT" + System.currentTimeMillis());
        }
        body.setId(null);
        return ApiResponse.ok(materialRepository.save(body));
    }

    @PutMapping("/materials/{id}")
    public ApiResponse<Material> updateMaterial(@PathVariable Long id, @RequestBody Material body) {
        Material m = materialRepository.findById(id).orElseThrow(() -> new BizException("物料不存在"));
        m.setCode(body.getCode());
        m.setName(body.getName());
        m.setCategory(body.getCategory());
        m.setSpec(body.getSpec());
        m.setUnit(body.getUnit());
        m.setSalePrice(body.getSalePrice());
        m.setCostPrice(body.getCostPrice());
        m.setSafeStock(body.getSafeStock());
        m.setStatus(body.getStatus());
        m.setRemark(body.getRemark());
        return ApiResponse.ok(materialRepository.save(m));
    }

    @DeleteMapping("/materials/{id}")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        materialRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/warehouses")
    public ApiResponse<List<Warehouse>> warehouses() {
        List<Warehouse> list = warehouseRepository.findAllByOrderByIdAsc();
        list.forEach(w -> {
            if (w.getManagerId() != null) {
                userRepository.findById(w.getManagerId()).ifPresent(u -> w.setManagerName(u.getRealName()));
            }
        });
        return ApiResponse.ok(list);
    }

    @PostMapping("/warehouses")
    public ApiResponse<Warehouse> createWarehouse(@RequestBody Warehouse body) {
        if (body.getCode() == null || body.getCode().isBlank()) {
            body.setCode("WH" + System.currentTimeMillis());
        }
        body.setId(null);
        return ApiResponse.ok(warehouseRepository.save(body));
    }

    @PutMapping("/warehouses/{id}")
    public ApiResponse<Warehouse> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse body) {
        Warehouse w = warehouseRepository.findById(id).orElseThrow(() -> new BizException("仓库不存在"));
        w.setCode(body.getCode());
        w.setName(body.getName());
        w.setLocation(body.getLocation());
        w.setManagerId(body.getManagerId());
        w.setStatus(body.getStatus());
        w.setRemark(body.getRemark());
        return ApiResponse.ok(warehouseRepository.save(w));
    }

    @DeleteMapping("/warehouses/{id}")
    public ApiResponse<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void enrichCustomer(Customer c) {
        if (c.getOwnerId() != null) {
            userRepository.findById(c.getOwnerId()).ifPresent(u -> c.setOwnerName(u.getRealName()));
        }
    }

    private void copyCustomer(Customer src, Customer dst) {
        dst.setCode(src.getCode());
        dst.setName(src.getName());
        dst.setShortName(src.getShortName());
        dst.setIndustry(src.getIndustry());
        dst.setLevel(src.getLevel());
        dst.setSource(src.getSource());
        dst.setStatus(src.getStatus());
        dst.setOwnerId(src.getOwnerId());
        dst.setPhone(src.getPhone());
        dst.setEmail(src.getEmail());
        dst.setAddress(src.getAddress());
        dst.setTaxNo(src.getTaxNo());
        dst.setCreditLimit(src.getCreditLimit());
        dst.setRemark(src.getRemark());
    }

    private void copySupplier(Supplier src, Supplier dst) {
        dst.setCode(src.getCode());
        dst.setName(src.getName());
        dst.setShortName(src.getShortName());
        dst.setCategory(src.getCategory());
        dst.setContactName(src.getContactName());
        dst.setPhone(src.getPhone());
        dst.setEmail(src.getEmail());
        dst.setAddress(src.getAddress());
        dst.setTaxNo(src.getTaxNo());
        dst.setBankName(src.getBankName());
        dst.setBankAccount(src.getBankAccount());
        dst.setLevel(src.getLevel());
        dst.setScore(src.getScore());
        dst.setStatus(src.getStatus());
        dst.setCooperationStart(src.getCooperationStart());
        dst.setRemark(src.getRemark());
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
