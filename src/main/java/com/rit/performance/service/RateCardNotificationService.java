package com.rit.performance.service;

import com.rit.performance.dto.RateCardResponse;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RateCardNotificationService {
    private final UserRepository users;
    private final AppUserDetailsService userDetails;
    private final AuditorAware<Long> auditor;
    private final ApplicationEventPublisher events;
    private final ApplicationEmailFactory emailFactory;

    public void changed(RateCardResponse card, RateCardResponse previous, String action) {
        String recipients = users.findAllByOrderByIdAsc().stream()
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()) && "FULL".equalsIgnoreCase(u.getPortalAccess()))
                .filter(u -> "ROLE_ADMIN".equals(userDetails.roleAuthority(u.getRole())))
                .map(u -> u.getEmployee() != null && u.getEmployee().getEmail() != null
                        && !u.getEmployee().getEmail().isBlank() ? u.getEmployee().getEmail() : u.getUsername())
                .filter(Objects::nonNull).filter(address -> !address.isBlank()).distinct()
                .collect(Collectors.joining(","));
        String actor = auditor.getCurrentAuditor().flatMap(users::findCurrentUser)
                .map(u -> u.getEmployee() == null ? u.getUsername()
                        : Objects.toString(u.getEmployee().getFirstName(), "") + " "
                        + Objects.toString(u.getEmployee().getLastName(), ""))
                .orElse("System");
        events.publishEvent(emailFactory.rateCardNotification(card, previous, action, recipients, actor));
    }
}
