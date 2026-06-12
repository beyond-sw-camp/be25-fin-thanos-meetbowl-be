package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
        when(userRepository.findByLoginId("admin")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("user1")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("user2")).thenReturn(Optional.empty());
        when(affiliateRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Affiliate value = invocation.getArgument(0);
                            return new Affiliate(
                                    java.util.UUID.randomUUID(),
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

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals(UserRole.ADMIN, captor.getAllValues().get(0).role());
        assertEquals(UserRole.USER, captor.getAllValues().get(1).role());
        assertEquals(UserRole.USER, captor.getAllValues().get(2).role());
    }

    @Test
    void doesNothingWhenAllLocalAccountsAlreadyExist() {
        AffiliateRepositoryPort affiliateRepository = mock(AffiliateRepositoryPort.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findByLoginId("admin")).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findByLoginId("user1")).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findByLoginId("user2")).thenReturn(Optional.of(mock(User.class)));

        new InitializeLocalAccountsUseCase(affiliateRepository, userRepository, passwordEncoder)
                .execute();

        verify(affiliateRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
