package com.hoang.ddd.application.booking.service;

import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TicketAccessPolicy {
    public void requireAdmin() {
        Authentication authentication = requireAuthentication();
        if (!isAdmin(authentication)) {
            throw denied();
        }
    }

    public void requireOwnerOrAdmin(Ticket ticket) {
        Authentication authentication = requireAuthentication();
        if (isAdmin(authentication)) {
            return;
        }

        if (ticket.getBooking() == null || ticket.getBooking().getUser() == null) {
            throw denied();
        }

        var principal = (UserDetailsCustom) authentication.getPrincipal();
        Long currentUserId = principal.getUser().getId();
        Long ownerId = ticket.getBooking().getUser().getId();
        if (!currentUserId.equals(ownerId)) {
            throw denied();
        }
    }

    private Authentication requireAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetailsCustom principal)
                || principal.getUser() == null || principal.getUser().getId() == null) {
            throw denied();
        }
        return authentication;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("Không có quyền xem vé");
    }
}
