package com.rit.performance.service;

import com.rit.performance.entity.Projects;
import com.rit.performance.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectsService {

    private final ProjectRepository repository;

    @Override
    public Projects save(Projects project) {
        return repository.save(project);
    }

    @Override
    public Projects update(Long id, Projects project) {

        Projects dbProject = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        dbProject.setProjectCode(project.getProjectCode());
        dbProject.setProjectName(project.getProjectName());
        dbProject.setDescription(project.getDescription());
        dbProject.setStartDate(project.getStartDate());
        dbProject.setEndDate(project.getEndDate());
        dbProject.setStatus(project.getStatus());
        dbProject.setDepartmentId(project.getDepartmentId());
        dbProject.setUpdatedBy(project.getUpdatedBy());

        return repository.save(dbProject);
    }

    @Override
    public Projects getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @Override
    public List<Projects> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}