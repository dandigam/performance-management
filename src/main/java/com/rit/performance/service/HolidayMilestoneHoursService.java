package com.rit.performance.service;

import com.rit.performance.entity.Holiday;
import com.rit.performance.entity.SowMilestonePosition;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.SowMilestonePositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayMilestoneHoursService {
    private static final BigDecimal DAILY_HOURS = new BigDecimal("8");
    private final SowRepository sows;
    private final SowMilestonePositionRepository milestonePositions;

    // Null means this holiday does not affect planned hours.
    public static LocalDate adjustmentDate(Holiday holiday) {
        if (holiday == null || !holiday.isActive() || holiday.getHolidayDate() == null
                || !isOnsite(holiday.getLocationType()))
            return null;
        LocalDate date = holiday.getHolidayDate();
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
            return null;
        return date;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void subtractHoursForAddedHoliday(Long clientId, LocalDate holidayDate) {
        var positions = findMatchingPositions(clientId, holidayDate);
        for (var position : positions) {
            adjustPositionHours(position, DAILY_HOURS.negate());
        }
        updateMilestoneTotals(positions);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreHoursForRemovedHoliday(Long clientId, LocalDate holidayDate) {
        var positions = findMatchingPositions(clientId, holidayDate);
        for (var position : positions) {
            adjustPositionHours(position, DAILY_HOURS);
        }
        updateMilestoneTotals(positions);
    }

    private void updateMilestoneTotals(List<SowMilestonePosition> positions) {
        positions.stream()
                .filter(position -> position.getHours() != null && !position.getHours().isBlank())
                .map(SowMilestonePosition::getMilestone).distinct()
                .forEach(milestone -> {
                    milestone.setAmount(milestonePositions.sumAmountByMilestoneId(milestone.getId()));
                    milestone.setEstimatedHours(totalMilestoneHours(milestone.getId()));
                });
    }

    private int totalMilestoneHours(Long milestoneId) {
        BigDecimal total = BigDecimal.ZERO;
        try {
            for (String value : milestonePositions.findHoursByMilestoneId(milestoneId)) {
                if (value == null || value.isBlank()) continue;
                BigDecimal hours = new BigDecimal(value.trim());
                if (hours.signum() < 0)
                    throw new InvalidOperationException("Negative position hours in milestone " + milestoneId);
                total = total.add(hours);
            }
            // estimated_hours is an integer; never silently truncate fractional hours.
            return total.intValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new InvalidOperationException("Cannot update estimated hours for milestone " + milestoneId
                    + ": position hours must be numeric and total a supported whole number.");
        }
    }

    private List<SowMilestonePosition> findMatchingPositions(Long clientId, LocalDate holidayDate) {
        if (clientId == null || holidayDate == null) return List.of();
        YearMonth month = YearMonth.from(holidayDate);
        List<SowMilestonePosition> positions = new ArrayList<>();
        for (var sow : sows.findDraftForHolidayMonths(
                clientId, month.atDay(1), month.atEndOfMonth())) {
            if (!"DRAFT".equalsIgnoreCase(sow.getStatus().getCode())) continue;
            positions.addAll(milestonePositions.findForHoliday(sow.getId(), holidayDate));
        }
        return positions;
    }

    private void adjustPositionHours(SowMilestonePosition position, BigDecimal hoursChange) {
        // An unplanned position has no hours to adjust.
        if (position.getHours() == null || position.getHours().isBlank()) return;
        BigDecimal hours;
        try {
            hours = new BigDecimal(position.getHours().trim());
        } catch (NumberFormatException exception) {
            throw new InvalidOperationException("Milestone position " + position.getId()
                    + " has non-numeric hours; correct its hours before changing the holiday.");
        }
        BigDecimal adjusted = hours.add(hoursChange);
        if (hours.signum() < 0 || adjusted.signum() < 0)
            throw new InvalidOperationException("Holiday change would leave invalid hours for milestone position "
                    + position.getId() + ". At least 8 hours are required for a holiday deduction.");
        String value = adjusted.stripTrailingZeros().toPlainString();
        if (value.length() > 50)
            throw new InvalidOperationException("Adjusted hours exceed the supported length for milestone position "
                    + position.getId());
        BigDecimal rate = position.getHourlyRate();
        if (rate == null || rate.signum() < 0)
            throw new InvalidOperationException("A non-negative hourly rate is required for milestone position "
                    + position.getId() + " before changing the holiday.");
        BigDecimal currentAmount = position.getAmount() == null ? hours.multiply(rate) : position.getAmount();
        BigDecimal amount = currentAmount.add(hoursChange.multiply(rate)).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() < 0 || amount.precision() > 15)
            throw new InvalidOperationException("Holiday change would leave an invalid amount for milestone position "
                    + position.getId());
        position.setHours(value);
        position.setAmount(amount);
    }

    private static boolean isOnsite(String value) {
        if (value == null) return false;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.equals("ONSITE") || normalized.equals("ONSHORE") || normalized.equals("ON_SHORE");
    }
}
