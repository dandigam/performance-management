package com.rit.performance.service;

import com.rit.performance.dto.CycleDetailsRequest;
import com.rit.performance.dto.CycleDetailsResponse;
import com.rit.performance.dto.AssessmentQuestionRequest;
import com.rit.performance.dto.AssessmentQuestionResponse;
import com.rit.performance.dto.AssessmentSectionRequest;
import com.rit.performance.dto.AssessmentSectionResponse;
import com.rit.performance.dto.AssessmentSetupRequest;
import com.rit.performance.dto.AssessmentSetupResponse;
import com.rit.performance.dto.PerformanceCycleAssessorRequest;
import com.rit.performance.dto.PerformanceCycleAssessorResponse;
import com.rit.performance.dto.PerformanceCycleRatingScaleResponse;
import com.rit.performance.dto.PerformanceCycleTimelineRequest;
import com.rit.performance.dto.PerformanceCycleTimelineResponse;
import com.rit.performance.dto.ReviewCycleRequest;
import com.rit.performance.dto.ReviewCycleResponse;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.PerformanceCycleAssessor;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.entity.PerformanceCycleTimeline;
import com.rit.performance.entity.PerformanceCycleQuestion;
import com.rit.performance.entity.PerformanceCycleRatingScale;
import com.rit.performance.entity.PerformanceCycleSection;
import com.rit.performance.mapper.CycleDetailsMapper;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.PerformanceCycleAssessorRepository;
import com.rit.performance.repository.PerformanceCycleConfigRepository;
import com.rit.performance.repository.PerformanceCycleTimelineRepository;
import com.rit.performance.repository.PerformanceCycleQuestionRepository;
import com.rit.performance.repository.PerformanceCycleSectionRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
@AllArgsConstructor
public class CycleDetailsServiceImpl implements CycleDetailsService {

    private final PerformanceCycleConfigRepository repository;
    private final PerformanceCycleAssessorRepository assessorRepository;
    private final LookupValueRepository lookupValueRepository;
    private final PerformanceCycleTimelineRepository timelineRepository;
    private final PerformanceCycleSectionRepository sectionRepository;
    private final PerformanceCycleQuestionRepository questionRepository;
    private final PerformanceCycleRatingScaleService ratingScaleService;
    private final ReviewCyclePublishService publishService;


    @Override
    public CycleDetailsResponse createCycleDetails(ReviewCycleRequest request) {
        CycleDetailsRequest cycleDetails = request.getCycleDetails();
        validateEvaluationDates(cycleDetails);
        PerformanceCycles saved;

        if (cycleDetails.getId() == null) {
            if (repository.existsByCycleName(cycleDetails.getCycleName())) {
                throw new IllegalArgumentException("cycleName must be unique");
            }
            PerformanceCycles newCycle = CycleDetailsMapper.toEntity(cycleDetails);
            applyEvaluationDates(newCycle, cycleDetails);
            saved = repository.save(newCycle);
        } else {
            PerformanceCycles existing = repository.findById(cycleDetails.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Performance cycle not found: " + cycleDetails.getId()));

            repository.findByCycleName(cycleDetails.getCycleName())
                    .filter(cycle -> !cycle.getId().equals(existing.getId()))
                    .ifPresent(cycle -> {
                        throw new IllegalArgumentException("cycleName must be unique");
                    });

            applyCycleDetails(existing, cycleDetails);
            saved = repository.save(existing);
        }

        if (request.getAssessors() != null && !request.getAssessors().isEmpty()) {
            if (cycleDetails.getId() != null) {
                assessorRepository.deleteByPerformanceCycleId(saved.getId());
            }
            List<PerformanceCycleAssessor> assessors = request.getAssessors().stream()
                    .map(assessor -> toAssessorEntity(saved.getId(), assessor))
                    .toList();
            assessorRepository.saveAll(assessors);
        }

        if (request.getTimelinePhases() != null && !request.getTimelinePhases().isEmpty()) {
            if (cycleDetails.getId() != null) {
                timelineRepository.deleteByPerformanceCycleId(saved.getId());
            }
            List<PerformanceCycleTimeline> timelinePhases = request.getTimelinePhases().stream()
                    .map(phase -> toTimelineEntity(saved.getId(), phase))
                    .toList();
            timelineRepository.saveAll(timelinePhases);
        }

        if (request.getAssessmentSetup() != null) {
            saveAssessmentSetup(saved.getId(), request.getAssessmentSetup());
        }

        if (request.getRatingScale() != null) {
            ratingScaleService.saveForCycle(saved.getId(), request.getRatingScale());
        }

        if (StringUtils.isNotBlank(request.getCycleDetails().getStatus()) && request.getCycleDetails().getStatus().equals("ACTIVE")) {
            publishService.publish(saved.getId(), null);
        }
        return toEnrichedResponse(saved);
    }

    private void saveAssessmentSetup(Long cycleId, AssessmentSetupRequest request) {
        if (request.getCycleId() != null && !request.getCycleId().equals(cycleId)) {
            throw new IllegalArgumentException("assessmentSetup.cycleId must match cycleDetails.id");
        }

        List<PerformanceCycleSection> existingSections =
                sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId);
        Set<Long> retainedSectionIds = new HashSet<>();
        for (AssessmentSectionRequest sectionRequest : request.getSections()) {
            PerformanceCycleSection section = findMatchingSection(existingSections, sectionRequest);
            if (section == null) {
                section = PerformanceCycleSection.builder().performanceCycleId(cycleId).build();
            }
            section.setSectionName(sectionRequest.getSectionName());
            section.setDisplayOrder(sectionRequest.getDisplayOrder());
            section.setActive(true);
            section = sectionRepository.save(section);
            retainedSectionIds.add(section.getId());
            reconcileQuestions(section, sectionRequest.getQuestions());
        }

        existingSections.stream().filter(section -> !retainedSectionIds.contains(section.getId()))
                .forEach(section -> {
                    section.setActive(false);
                    sectionRepository.save(section);
                    List<PerformanceCycleQuestion> questions = questionRepository
                            .findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(section.getId());
                    questions.forEach(question -> question.setActive(false));
                    questionRepository.saveAll(questions);
                });
    }

    private PerformanceCycleSection findMatchingSection(List<PerformanceCycleSection> existing,
            AssessmentSectionRequest request) {
        if (request.getId() != null) {
            return existing.stream().filter(section -> section.getId().equals(request.getId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "Section " + request.getId() + " does not belong to this cycle"));
        }
        return existing.stream()
                .filter(section -> section.getSectionName().equalsIgnoreCase(request.getSectionName()))
                .findFirst()
                .or(() -> existing.stream()
                        .filter(section -> section.getDisplayOrder().equals(request.getDisplayOrder())).findFirst())
                .orElse(null);
    }

    private void reconcileQuestions(PerformanceCycleSection section, List<AssessmentQuestionRequest> requests) {
        List<PerformanceCycleQuestion> existing = questionRepository
                .findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(section.getId());
        Set<Long> retainedIds = new HashSet<>();
        for (AssessmentQuestionRequest request : requests) {
            PerformanceCycleQuestion question = findMatchingQuestion(existing, request);
            if (question == null) question = toQuestionEntity(section.getId(), request);
            question.setPerformanceCycleSectionId(section.getId());
            question.setQuestionText(request.getQuestionText());
            question.setResponseType(request.getQuestionType());
            question.setRequired(request.getRequired());
            question.setAllowComments(request.getAllowComments());
            question.setDisplayOrder(request.getDisplayOrder());
            question.setActive(true);
            question = questionRepository.save(question);
            retainedIds.add(question.getId());
        }
        existing.stream().filter(question -> !retainedIds.contains(question.getId()))
                .forEach(question -> {
                    question.setActive(false);
                    questionRepository.save(question);
                });
    }

    private PerformanceCycleQuestion findMatchingQuestion(List<PerformanceCycleQuestion> existing,
            AssessmentQuestionRequest request) {
        if (request.getId() != null) {
            return existing.stream().filter(question -> question.getId().equals(request.getId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "Question " + request.getId() + " does not belong to this section"));
        }
        return existing.stream()
                .filter(question -> question.getQuestionText().equalsIgnoreCase(request.getQuestionText()))
                .findFirst()
                .or(() -> existing.stream()
                        .filter(question -> question.getDisplayOrder().equals(request.getDisplayOrder())).findFirst())
                .orElse(null);
    }

    private PerformanceCycleQuestion toQuestionEntity(Long sectionId,
                                                        AssessmentQuestionRequest request) {
        return PerformanceCycleQuestion.builder()
                .performanceCycleSectionId(sectionId)
                .questionText(request.getQuestionText())
                .responseType(request.getQuestionType())
                .required(request.getRequired())
                .allowComments(request.getAllowComments())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    private PerformanceCycleTimeline toTimelineEntity(Long performanceCycleId,
                                                       PerformanceCycleTimelineRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException(
                    "Timeline endDate must be on or after startDate for phase: " + request.getPhaseName());
        }
        return PerformanceCycleTimeline.builder()
                .performanceCycleId(performanceCycleId)
                .phaseName(request.getPhaseName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .roleId(request.getRoleId())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    private void applyCycleDetails(PerformanceCycles existing, CycleDetailsRequest request) {
        existing.setCycleName(request.getCycleName());
        applyEvaluationDates(existing, request);
        existing.setDescription(request.getDescription());
        existing.setReviewTypeId(request.getReviewTypeId());
        existing.setApplicableTypeId(request.getApplicableTypeId());
        existing.setScopeValueIds(request.getScopeValueIds());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            existing.setStatus(request.getStatus());
        }
    }

    private PerformanceCycleAssessor toAssessorEntity(Long performanceCycleId,
                                                       PerformanceCycleAssessorRequest request) {
        LookupValue role = lookupValueRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid roleId: " + request.getRoleId()));
        if (!lookupValueRepository.existsById(request.getActionTypeId())) {
            throw new IllegalArgumentException("Invalid actionTypeId: " + request.getActionTypeId());
        }

        return PerformanceCycleAssessor.builder()
                .performanceCycleId(performanceCycleId)
                .assessorName(role.getName())
                .roleId(request.getRoleId())
                .actionTypeId(request.getActionTypeId())
                .weightage(request.getWeightage())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    @Override
    public CycleDetailsResponse updateCycleDetails(Long id, CycleDetailsRequest request) {
        validateEvaluationDates(request);
        PerformanceCycles existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Performance cycle not found"));

        if (!existing.getCycleName().equals(request.getCycleName())
                && repository.existsByCycleName(request.getCycleName())) {
            throw new IllegalArgumentException("cycleName must be unique");
        }

        applyCycleDetails(existing, request);

        return toEnrichedResponse(repository.save(existing));
    }

    @Override
    public List<CycleDetailsResponse> getAllCycleDetails() {
        return repository.findAll().stream()
                .map(this::toEnrichedResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewCycleResponse> getAllReviewCycles() {
        return repository.findAll().stream()
                .map(cycle -> getReviewCycleById(cycle.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewCycleResponse getReviewCycleById(Long id) {
        PerformanceCycles cycle = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Performance cycle not found"));

        List<PerformanceCycleAssessorResponse> assessors = assessorRepository
                .findByPerformanceCycleIdOrderByDisplayOrderAsc(id).stream()
                .map(a -> PerformanceCycleAssessorResponse.builder()
                        .id(a.getId())
                        .roleId(a.getRoleId())
                        .assessorName(a.getAssessorName())
                        .actionTypeId(a.getActionTypeId())
                        .weightage(a.getWeightage())
                        .displayOrder(a.getDisplayOrder())
                        .build())
                .toList();

        List<PerformanceCycleTimelineResponse> timelinePhases = timelineRepository
                .findByPerformanceCycleIdOrderByDisplayOrderAsc(id).stream()
                .map(t -> PerformanceCycleTimelineResponse.builder()
                        .id(t.getId())
                        .phaseName(t.getPhaseName())
                        .description(t.getDescription())
                        .startDate(t.getStartDate())
                        .endDate(t.getEndDate())
                        .roleId(t.getRoleId())
                        .displayOrder(t.getDisplayOrder())
                        .build())
                .toList();

        AssessmentSetupResponse assessmentSetup = buildAssessmentSetup(id);

        PerformanceCycleRatingScale ratingScale = ratingScaleService.findByCycleId(id).orElse(null);
        PerformanceCycleRatingScaleResponse ratingScaleResponse = ratingScale == null ? null
                : PerformanceCycleRatingScaleResponse.builder()
                        .id(ratingScale.getId())
                        .cycleId(ratingScale.getPerformanceCycleId())
                        .scaleName(ratingScale.getScaleName())
                        .ratingScaleId(ratingScale.getRatingScaleId())
                        .active(ratingScale.getActive())
                        .build();

        return ReviewCycleResponse.builder()
                .cycleDetails(toEnrichedResponse(cycle))
                .assessors(assessors)
                .timelinePhases(timelinePhases)
                .assessmentSetup(assessmentSetup)
                .ratingScale(ratingScaleResponse)
                .build();
    }

    private CycleDetailsResponse toEnrichedResponse(PerformanceCycles cycle) {
        CycleDetailsResponse response = CycleDetailsMapper.toResponse(cycle);

        lookupValueRepository.findById(cycle.getReviewTypeId())
                .filter(value -> value.getLookupType() != null
                        && "REVIEW_TYPE".equalsIgnoreCase(value.getLookupType().getCode()))
                .ifPresent(value -> response.setReviewTypeName(value.getName()));

        return response;
    }

    private void validateEvaluationDates(CycleDetailsRequest request) {
        if (request.getEvaluationStartDate() == null || request.getEvaluationEndDate() == null) {
            throw new IllegalArgumentException(
                    "evaluationStartDate and evaluationEndDate are required");
        }
        if (request.getEvaluationEndDate().isBefore(request.getEvaluationStartDate())) {
            throw new IllegalArgumentException("evaluationEndDate cannot be before evaluationStartDate");
        }
    }

    private void applyEvaluationDates(PerformanceCycles cycle, CycleDetailsRequest request) {
        cycle.setEvaluationStartDate(request.getEvaluationStartDate());
        cycle.setEvaluationEndDate(request.getEvaluationEndDate());
    }

    private AssessmentSetupResponse buildAssessmentSetup(Long cycleId) {
        List<PerformanceCycleSection> sections =
                sectionRepository.findByPerformanceCycleIdOrderByDisplayOrderAsc(cycleId);
        if (sections.isEmpty()) {
            return null;
        }

        List<AssessmentSectionResponse> sectionResponses = sections.stream()
                .map(section -> {
                    List<AssessmentQuestionResponse> questionResponses = questionRepository
                            .findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(section.getId()).stream()
                            .map(q -> AssessmentQuestionResponse.builder()
                                    .id(q.getId())
                                    .questionText(q.getQuestionText())
                                    .questionType(q.getResponseType())
                                    .required(q.getRequired())
                                    .allowComments(q.getAllowComments())
                                    .displayOrder(q.getDisplayOrder())
                                    .build())
                            .toList();
                    return AssessmentSectionResponse.builder()
                            .id(section.getId())
                            .sectionName(section.getSectionName())
                            .displayOrder(section.getDisplayOrder())
                            .questions(questionResponses)
                            .build();
                })
                .toList();

        return AssessmentSetupResponse.builder()
                .cycleId(cycleId)
                .sections(sectionResponses)
                .build();
    }
}
