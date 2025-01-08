package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.service.EccnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eccn")
@RequiredArgsConstructor
@Tag(name = "ECCN Management", description = "ECCN Management API")
public class EccnController {

    private final EccnService eccnService;

    @PostMapping
    @Operation(summary = "Create ECCN record", description = "Create a new ECCN classification record")
    public Eccn createEccn(@RequestBody Eccn eccn) {
        return eccnService.createEccn(eccn);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ECCN record", description = "Update an existing ECCN classification record")
    public Eccn updateEccn(@PathVariable String id, @RequestBody Eccn eccn) {
        return eccnService.updateEccn(id, eccn);
    }

    @GetMapping("/commodity-code/{commodityCode}")
    @Operation(summary = "Find by commodity code", description = "Find ECCN records by commodity code")
    public List<Eccn> findByCommodityCode(@PathVariable String commodityCode) {
        return eccnService.findByCommodityCode(commodityCode);
    }

    @GetMapping("/financial-software")
    @Operation(summary = "Get financial software ECCNs", description = "Get ECCN records for financial software")
    public List<Eccn> getFinancialSoftwareEccns() {
        return eccnService.findFinancialSoftwareEccns();
    }

    @GetMapping("/data-analytics")
    @Operation(summary = "Find ECCNs with data analytics capabilities", description = "Find ECCN records with specific data analytics capabilities")
    public List<Eccn> findDataAnalyticsEccns(@RequestParam List<String> capabilities) {
        return eccnService.findDataAnalyticsEccns(capabilities);
    }

    @GetMapping("/ear-controls")
    @Operation(summary = "Find ECCNs by EAR controls", description = "Find ECCN records by applicable EAR controls")
    public List<Eccn> findEccnsByEARControls(@RequestParam List<String> earControls) {
        return eccnService.findEccnsByEARControls(earControls);
    }

    @GetMapping("/status")
    @Operation(summary = "Get service status", description = "Returns the status of the ECCN Management Service")
    public String getStatus() {
        return "ECCN Management Service is running";
    }
}