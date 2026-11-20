package com.ebmp.contract;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.BizException;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.masterdata.SupplierRepository;
import com.ebmp.system.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractRepository contractRepository;
    private final ContractService contractService;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<Contract>> list(@RequestParam(required = false) String type) {
        List<Contract> list = type == null || type.isBlank()
                ? contractRepository.findAllByOrderByIdDesc()
                : contractRepository.findByTypeOrderByIdDesc(type);
        list.forEach(contractService::enrich);
        return ApiResponse.ok(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<Contract> detail(@PathVariable Long id) {
        Contract c = contractRepository.findById(id).orElseThrow(() -> new BizException("合同不存在"));
        contractService.enrich(c);
        return ApiResponse.ok(c);
    }

    @PostMapping
    public ApiResponse<Contract> create(@RequestBody Contract body) {
        return ApiResponse.ok(contractService.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<Contract> update(@PathVariable Long id, @RequestBody Contract body) {
        Contract c = contractRepository.findById(id).orElseThrow(() -> new BizException("合同不存在"));
        c.setTitle(body.getTitle());
        c.setType(body.getType());
        c.setPartyId(body.getPartyId());
        c.setAmount(body.getAmount());
        c.setStartDate(body.getStartDate());
        c.setEndDate(body.getEndDate());
        c.setRemark(body.getRemark());
        return ApiResponse.ok(contractRepository.save(c));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Contract> submit(@PathVariable Long id) {
        return ApiResponse.ok(contractService.submit(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contractRepository.deleteById(id);
        return ApiResponse.ok();
    }
}
