package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;

class InitializeLocalAccountsUseCaseTest {

    @Test
    void createsAdminAndUserWhenAdminDoesNotExist() {
        AffiliateRepositoryPort affiliateRepository = mock(AffiliateRepositoryPort.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findByLoginId("admin1")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("admin2")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("user1")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("user2")).thenReturn(Optional.empty());
        when(affiliateRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Affiliate value = invocation.getArgument(0);
                            return new Affiliate(
                                    UUID.randomUUID(),
                                    value.name(),
                                    value.code(),
                                    value.status(),
                                    value.sortOrder(),
                                    value.createdAt(),
                                    value.updatedAt());
                        });
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");

        new InitializeLocalAccountsUseCase(affiliateRepository, userRepository, passwordEncoder)
                .execute();

        ArgumentCaptor<Affiliate> affiliateCaptor = ArgumentCaptor.forClass(Affiliate.class);
        verify(affiliateRepository, org.mockito.Mockito.times(2)).save(affiliateCaptor.capture());
        assertEquals("기본 계열사 1", affiliateCaptor.getAllValues().get(0).name());
        assertEquals("기본 계열사 2", affiliateCaptor.getAllValues().get(1).name());
        assertEquals("LOCAL-1", affiliateCaptor.getAllValues().get(0).code());
        assertEquals("LOCAL-2", affiliateCaptor.getAllValues().get(1).code());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(4)).save(captor.capture());
        assertEquals(UserRole.ADMIN, captor.getAllValues().get(0).role());
        assertEquals(UserRole.ADMIN, captor.getAllValues().get(1).role());
        assertEquals(UserRole.USER, captor.getAllValues().get(2).role());
        assertEquals(UserRole.USER, captor.getAllValues().get(3).role());
        assertEquals("admin1", captor.getAllValues().get(0).loginId());
        assertEquals("admin2", captor.getAllValues().get(1).loginId());
        assertEquals(captor.getAllValues().get(0).affiliateId(), captor.getAllValues().get(2).affiliateId());
        assertEquals(captor.getAllValues().get(1).affiliateId(), captor.getAllValues().get(3).affiliateId());
        assertNotEquals(captor.getAllValues().get(0).affiliateId(), captor.getAllValues().get(1).affiliateId());
    }

    @Test
    void doesNothingWhenAllLocalAccountsAlreadyExist() {
        AffiliateRepositoryPort affiliateRepository = mock(AffiliateRepositoryPort.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findByLoginId("admin1")).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findByLoginId("admin2")).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findByLoginId("user1")).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findByLoginId("user2")).thenReturn(Optional.of(mock(User.class)));

        new InitializeLocalAccountsUseCase(affiliateRepository, userRepository, passwordEncoder)
                .execute();

        verify(affiliateRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
