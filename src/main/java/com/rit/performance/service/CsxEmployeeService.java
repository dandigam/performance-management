package com.rit.performance.service;

import com.rit.performance.dto.CsxEmployeeResponse;
import com.rit.performance.dto.CsxEmployeeCreateRequest;
import com.rit.performance.dto.CsxEmployeeUpdateRequest;

import java.util.List;

public interface CsxEmployeeService {
    CsxEmployeeResponse create(CsxEmployeeCreateRequest request);
    CsxEmployeeResponse update(Long id, CsxEmployeeUpdateRequest request);
    List<CsxEmployeeResponse> getAll();
    CsxEmployeeResponse getById(Long id);
}
