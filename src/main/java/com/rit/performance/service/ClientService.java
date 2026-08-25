package com.rit.performance.service;

import com.rit.performance.dto.ClientRequest;
import com.rit.performance.dto.ClientResponse;

import java.util.List;

public interface ClientService {
    ClientResponse create(ClientRequest request);
    ClientResponse update(Long id, ClientRequest request);
    ClientResponse getById(Long id);
    List<ClientResponse> getAll();
    void delete(Long id);
}
