package com.rit.performance.service;

import com.rit.performance.dto.HolidayRequest;
import com.rit.performance.dto.HolidayResponse;
import com.rit.performance.entity.Holiday;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HolidayService {
    private final HolidayRepository repository;
    private final HolidayMilestoneHoursService milestoneHours;
    private final com.rit.performance.repository.ClientRepository clients;

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        var client = findClient(request.getClientId());
        String location = normalizeLocation(request.getLocationType());
        if (repository.existsByClient_IdAndLocationTypeIgnoreCaseAndHolidayDate(
                client.getId(), location, request.getHolidayDate())) {
            throw new DuplicateResourceException("A holiday already exists for " + location
                    + " on " + request.getHolidayDate());
        }
        Holiday holiday = new Holiday();
        holiday.setClient(client);
        apply(holiday, request, location);
        holiday.setActive(request.getActive() == null || request.getActive());
        Holiday saved = repository.save(holiday);
        milestoneHours.subtractHoursForAddedHoliday(client.getId(), HolidayMilestoneHoursService.adjustmentDate(saved));
        return toResponse(saved);
    }

    @Transactional
    public HolidayResponse update(Long id, HolidayRequest request) {
        Holiday holiday = findForUpdate(id);
        Long oldClientId = clientId(holiday);
        var client = findClient(request.getClientId());
        LocalDate oldDate = HolidayMilestoneHoursService.adjustmentDate(holiday);
        String location = normalizeLocation(request.getLocationType());
        if (repository.existsByClient_IdAndLocationTypeIgnoreCaseAndHolidayDateAndIdNot(
                client.getId(), location, request.getHolidayDate(), id)) {
            throw new DuplicateResourceException("A holiday already exists for " + location
                    + " on " + request.getHolidayDate());
        }
        holiday.setClient(client);
        apply(holiday, request, location);
        if (request.getActive() != null) holiday.setActive(request.getActive());
        Holiday saved = repository.save(holiday);
        LocalDate newDate = HolidayMilestoneHoursService.adjustmentDate(saved);
        if (!Objects.equals(oldDate, newDate) || !Objects.equals(oldClientId, client.getId())) {
            milestoneHours.restoreHoursForRemovedHoliday(oldClientId, oldDate);
            milestoneHours.subtractHoursForAddedHoliday(client.getId(), newDate);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public HolidayResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> getAll(Integer year, String locationType, Boolean active) {
        return getAll(null, year, locationType, active);
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> getAll(Long clientId, Integer year, String locationType, Boolean active) {
        if (clientId != null) findClient(clientId);
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate start = LocalDate.of(selectedYear, 1, 1);
        LocalDate end = LocalDate.of(selectedYear, 12, 31);
        String location = locationType == null || locationType.isBlank() ? null : normalizeLocation(locationType);
        List<Holiday> holidays = repository.findForCalendar(clientId, start, end, location, active);
        return holidays.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Holiday holiday = findForUpdate(id);
        if (repository.isReferencedByEntries(id) || repository.isReferencedByScheduledDays(id)) {
            throw new InvalidOperationException("Holiday is used by timesheet records and cannot be deleted. "
                    + "Set active to false to disable it instead.");
        }
        milestoneHours.restoreHoursForRemovedHoliday(clientId(holiday), HolidayMilestoneHoursService.adjustmentDate(holiday));
        repository.delete(holiday);
        repository.flush();
    }

    private Holiday find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
    }

    private Long clientId(Holiday holiday) {
        return holiday.getClient() == null ? null : holiday.getClient().getId();
    }

    private com.rit.performance.entity.Client findClient(Long id) {
        if (id == null || id <= 0) throw new InvalidOperationException("A positive clientId is required");
        return clients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    private Holiday findForUpdate(Long id) {
        return repository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
    }

    private void apply(Holiday holiday, HolidayRequest request, String location) {
        holiday.setHolidayName(request.getHolidayName().trim());
        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setLocationType(location);
        holiday.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
    }

    private String normalizeLocation(String value) {
        String location = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("ONSHORE".equals(location) || "ON_SHORE".equals(location)) location = "ONSITE";
        if ("OFF_SHORE".equals(location)) location = "OFFSHORE";
        if (!List.of("ONSITE", "OFFSHORE").contains(location)) {
            throw new InvalidOperationException("locationType must be ONSITE/ONSHORE or OFFSHORE");
        }
        return location;
    }

    private HolidayResponse toResponse(Holiday holiday) {
        return HolidayResponse.builder()
                .id(holiday.getId())
                .clientId(clientId(holiday))
                .clientName(holiday.getClient() == null ? null : holiday.getClient().getClientName())
                .holidayName(holiday.getHolidayName())
                .holidayDate(holiday.getHolidayDate())
                .locationType(holiday.getLocationType())
                .description(holiday.getDescription())
                .active(holiday.isActive())
                .createdBy(holiday.getCreatedBy())
                .createdOn(holiday.getCreatedOn())
                .updatedBy(holiday.getUpdatedBy())
                .updatedOn(holiday.getUpdatedOn())
                .build();
    }
}
