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

@Service
@RequiredArgsConstructor
public class HolidayService {
    private final HolidayRepository repository;

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        String location = normalizeLocation(request.getLocationType());
        if (repository.existsByLocationTypeIgnoreCaseAndHolidayDate(location, request.getHolidayDate())) {
            throw new DuplicateResourceException("A holiday already exists for " + location
                    + " on " + request.getHolidayDate());
        }
        Holiday holiday = new Holiday();
        apply(holiday, request, location);
        holiday.setActive(request.getActive() == null || request.getActive());
        return toResponse(repository.save(holiday));
    }

    @Transactional
    public HolidayResponse update(Long id, HolidayRequest request) {
        Holiday holiday = find(id);
        String location = normalizeLocation(request.getLocationType());
        if (repository.existsByLocationTypeIgnoreCaseAndHolidayDateAndIdNot(
                location, request.getHolidayDate(), id)) {
            throw new DuplicateResourceException("A holiday already exists for " + location
                    + " on " + request.getHolidayDate());
        }
        apply(holiday, request, location);
        if (request.getActive() != null) holiday.setActive(request.getActive());
        return toResponse(repository.save(holiday));
    }

    @Transactional(readOnly = true)
    public HolidayResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> getAll(Integer year, String locationType, Boolean active) {
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate start = LocalDate.of(selectedYear, 1, 1);
        LocalDate end = LocalDate.of(selectedYear, 12, 31);
        List<Holiday> holidays = locationType == null || locationType.isBlank()
                ? repository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end)
                : repository.findByLocationTypeIgnoreCaseAndHolidayDateBetweenOrderByHolidayDateAsc(
                        normalizeLocation(locationType), start, end);
        return holidays.stream()
                .filter(holiday -> active == null || holiday.isActive() == active)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Holiday holiday = find(id);
        holiday.setActive(false);
        repository.save(holiday);
    }

    private Holiday find(Long id) {
        return repository.findById(id)
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
