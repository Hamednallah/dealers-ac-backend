package com.dealersac.inventory.dealer.application;

import com.dealersac.inventory.dealer.dto.DealerPatchRequest;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.dealersac.inventory.dealer.dto.DealerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DealerService {

    DealerResponse create(DealerRequest request, String tenantId);

    DealerResponse findById(UUID id, String tenantId);

    Page<DealerResponse> findAll(String tenantId, Pageable pageable);

    DealerResponse update(UUID id, DealerPatchRequest patch, String tenantId);

    void delete(UUID id, String tenantId);
}
