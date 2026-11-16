package com.hoang.ddd.application.booking.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.ddd.application.booking.service.TicketAccessPolicy;
import com.hoang.ddd.application.booking.service.TicketResponseCache;
import com.hoang.ddd.domain.model.entity.*;
import com.hoang.ddd.domain.repository.TicketRepository;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(TicketAccessIntegrationTest.Config.class)
class TicketAccessIntegrationTest {
    @Configuration
    @EnableCaching
    static class Config {
        @Bean TicketRepository repository() { return mock(TicketRepository.class); }
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("ticket_detail", "trip_tickets");
        }
        @Bean TicketAccessPolicy policy() { return new TicketAccessPolicy(); }
        @Bean TicketResponseCache responses(CacheManager cache) {
            return new TicketResponseCache(cache);
        }
        @Bean GetTicketByIdUseCase byId(TicketRepository repo, TicketAccessPolicy policy, TicketResponseCache cache) {
            return new GetTicketByIdUseCase(repo, policy, cache);
        }
        @Bean GetTicketByCodeUseCase byCode(TicketRepository repo, TicketAccessPolicy policy, TicketResponseCache cache) {
            return new GetTicketByCodeUseCase(repo, policy, cache);
        }
        @Bean GetTicketsPageUseCase page(TicketRepository repo, TicketAccessPolicy policy) {
            return new GetTicketsPageUseCase(repo, policy);
        }
        @Bean GetTripTicketsUseCase seats(TicketRepository repo) { return new GetTripTicketsUseCase(repo); }
    }

    @Autowired TicketRepository repository;
    @Autowired CacheManager cacheManager;
    @Autowired GetTicketByIdUseCase byId;
    @Autowired GetTicketByCodeUseCase byCode;
    @Autowired GetTicketsPageUseCase page;
    @Autowired GetTripTicketsUseCase seats;
    Ticket ticket;

    @BeforeEach
    void prepare() {
        reset(repository);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        ticket = Ticket.builder().id(7L).ticketCode("TK-A")
                .booking(Booking.builder().id(4L).user(User.builder().id(1L).build()).build())
                .seat(Seat.builder().id(3L).seatNumber("A1").build())
                .passengerName("Private passenger").passengerId("Private ID").build();
        when(repository.findById(7L)).thenReturn(Optional.of(ticket));
        when(repository.findByTicketCode("TK-A")).thenReturn(Optional.of(ticket));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    void login(long id, String role) {
        var user = User.builder().id(id).roles(Set.of(Role.builder().name(role).build())).build();
        var principal = new UserDetailsCustom(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void ownerWarmsCacheButOtherUserCannotReadByIdOrCode() {
        login(1, "ROLE_USER");
        var cached = byId.execute(7L);
        assertEquals("Private passenger", cached.getPassengerName());
        assertSame(cached, byCode.execute("TK-A"));
        login(2, "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> byId.execute(7L));
        assertThrows(AccessDeniedException.class, () -> byCode.execute("TK-A"));
        assertThrows(AccessDeniedException.class, () -> page.execute(null, null, null));
        verify(repository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void adminWarmsCacheButOtherUserStillCannotRead() {
        login(9, "ROLE_ADMIN");
        assertNotNull(byId.execute(7L));
        login(2, "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> byCode.execute("TK-A"));
    }

    @Test
    void ownershipIsCheckedAgainAfterBookingChanges() {
        login(1, "ROLE_USER");
        byId.execute(7L);
        ticket.setBooking(Booking.builder().id(5L).user(User.builder().id(2L).build()).build());
        assertThrows(AccessDeniedException.class, () -> byId.execute(7L));
        login(2, "ROLE_USER");
        assertEquals(5L, byId.execute(7L).getBookingId());
    }

    @Test
    void missingOwnerAndAnonymousAreDeniedButAdminCanInspectUnbookedTicket() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> byId.execute(7L));
        login(1, "ROLE_USER");
        ticket.getBooking().setUser(null);
        assertThrows(AccessDeniedException.class, () -> byId.execute(7L));
        ticket.setBooking(null);
        assertThrows(AccessDeniedException.class, () -> byId.execute(7L));
        login(9, "ROLE_ADMIN");
        assertNotNull(byId.execute(7L));
    }

    @Test
    void adminCanReadDetailedTicketList() {
        login(9, "ROLE_ADMIN");
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(ticket)));
        assertEquals("Private passenger", page.execute(pageable, null, null)
                .getContent().getFirst().getPassengerName());
    }

    @Test
    void seatMapAndItsCacheContainOnlyPublicFields() throws Exception {
        cacheManager.getCache("trip_tickets").put(8L, List.of(
                com.hoang.ddd.application.booking.dto.response.TicketResponse.of(ticket)));
        when(repository.findByTripId(8L)).thenReturn(List.of(ticket));
        var result = seats.execute(8L);
        assertSame(result, seats.execute(8L));
        var json = new ObjectMapper().valueToTree(result.getFirst());
        assertEquals(7L, json.get("ticketId").asLong());
        for (String field : List.of("passengerName", "passengerId", "passengerType", "bookingId", "ticketCode")) {
            assertFalse(json.has(field), field);
        }
        verify(repository, times(1)).findByTripId(8L);
    }
}
