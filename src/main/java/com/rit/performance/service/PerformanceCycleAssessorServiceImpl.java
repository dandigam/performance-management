package com.rit.performance.service;

import com.rit.performance.entity.PerformanceCycleAssessor;
import com.rit.performance.repository.PerformanceCycleAssessorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PerformanceCycleAssessorServiceImpl implements PerformanceCycleAssessorService {

    private final PerformanceCycleAssessorRepository repository;

    public PerformanceCycleAssessorServiceImpl(PerformanceCycleAssessorRepository repository) {
        this.repository = repository;
    }

    @Override
    public PerformanceCycleAssessor create(PerformanceCycleAssessor assessor) {
        if (assessor.getWeightage() == null) {
            assessor.setWeightage(BigDecimal.ZERO);
        }
        if (assessor.getActive() == null) {
            assessor.setActive(true);
        }
        return repository.save(assessor);
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceCycleAssessor getById(Long id) {
        return findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerformanceCycleAssessor> getByPerformanceCycleId(Long performanceCycleId) {
        return repository.findByPerformanceCycleIdOrderByDisplayOrderAsc(performanceCycleId);
    }

    @Override
    public PerformanceCycleAssessor update(Long id, PerformanceCycleAssessor assessor) {
        PerformanceCycleAssessor existing = findById(id);
        existing.setPerformanceCycleId(assessor.getPerformanceCycleId());
        existing.setAssessorName(assessor.getAssessorName());
        existing.setRoleId(assessor.getRoleId());
        existing.setActionTypeId(assessor.getActionTypeId());
        existing.setWeightage(assessor.getWeightage() == null ? BigDecimal.ZERO : assessor.getWeightage());
        existing.setDisplayOrder(assessor.getDisplayOrder());
        existing.setActive(assessor.getActive() == null ? existing.getActive() : assessor.getActive());
        existing.setUpdatedBy(assessor.getUpdatedBy());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private PerformanceCycleAssessor findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PerformanceCycleAssessor not found"));
    }
}
