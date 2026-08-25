package com.rit.performance.service;

import com.rit.performance.dto.request.WorkOrderRequest;
import com.rit.performance.dto.response.WorkOrderResponse;

import java.util.List;

public interface WorkOrderService {
    WorkOrderResponse create(WorkOrderRequest request);
    List<WorkOrderResponse> getAll();
    WorkOrderResponse getById(Long id);
    WorkOrderResponse update(Long id, WorkOrderRequest request);
    void delete(Long id);
}
