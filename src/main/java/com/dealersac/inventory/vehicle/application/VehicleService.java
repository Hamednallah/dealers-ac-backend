package com.dealersac.inventory.vehicle.application;

import com.dealersac.inventory.vehicle.dto.VehicleFilter;
import com.dealersac.inventory.vehicle.dto.VehiclePatchRequest;
import com.dealersac.inventory.vehicle.dto.VehicleRequest;
import com.dealersac.inventory.vehicle.dto.VehicleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VehicleService {
    VehicleResponse create(VehicleRequest request, String tenantId);
    VehicleResponse findById(UUID id, String tenantId);
    Page<VehicleResponse> findAll(VehicleFilter filter, Pageable pageable);
    VehicleResponse update(UUID id, VehiclePatchRequest patch, String tenantId);
    void delete(UUID id, String tenantId);
}
