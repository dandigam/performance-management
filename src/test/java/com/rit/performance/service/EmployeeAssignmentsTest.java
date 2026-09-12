package com.rit.performance.service;

import com.rit.performance.dto.EmployeeAssignmentResponse;
import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.SowMilestonePositionAssignmentRepository;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAssignmentsTest {
    @Mock SowRepository sowRepository;
    @Mock SowMilestonePositionAssignmentRepository milestonePositionAssignmentRepository;
    @Mock UserRepository userRepository;
    @Spy @InjectMocks EmployeeServiceImpl service;

    @Test
    void assignmentsRequireActiveOrCompletedSow() {
        var codes = List.of("DRAFT", "ACTIVE", "completed", "ON_HOLD", "CANCELLED", "APPROVED");
        var sows = IntStream.range(0, codes.size()).mapToObj(index -> {
            var status = new LookupValue();
            status.setCode(codes.get(index));
            var sow = new Sow();
            sow.setId((long) index + 1);
            sow.setStatus(status);
            return sow;
        }).toList();
        var assignments = sows.stream().map(sow -> EmployeeAssignmentResponse.builder()
                .assignmentId(sow.getId()).sowId(sow.getId())
                .sowCode("SOW-" + sow.getId()).assignmentStatus("ACTIVE").build()).toList();
        doReturn(EmployeeBasicInfoResponse.builder().employeeId(3L)
                .employeeName("Employee").assignmentList(assignments).build())
                .when(service).getById(3L);
        when(sowRepository.findAllById(any())).thenReturn(sows);

        var response = service.getAssignmentsByEmployeeId(3L);

        assertThat(response.getAssignmentList()).extracting(item -> item.getSowId())
                .containsExactly(2L, 3L);
        assertThat(response.getAssignmentList().get(0).getSowCode()).isEqualTo("SOW-2");
    }
}
