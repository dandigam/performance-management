package com.rit.performance.service;

import com.rit.performance.entity.Projects;

import java.util.List;

public interface ProjectsService {

    Projects save(Projects project);

    Projects update(Long id, Projects project);

    Projects getById(Long id);

    List<Projects> getAll();

    void delete(Long id);
}