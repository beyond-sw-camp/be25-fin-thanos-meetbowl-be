package com.meetbowl.application.user;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.user.UserSearchIndexPort;

class UserSearchReindexUseCaseTest {

    private final UserSearchIndexPort userSearchIndexPort =
            org.mockito.Mockito.mock(UserSearchIndexPort.class);
    private final UserSearchReindexUseCase useCase =
            new UserSearchReindexUseCase(userSearchIndexPort);

    @Test
    void executeIndexesEachDistinctUserWhenUserIdsProvided() {
        UUID userId = UUID.randomUUID();

        useCase.execute(
                new UserSearchReindexUseCase.Command(
                        false,
                        new LinkedHashSet<>(Arrays.asList(userId, userId)),
                        null,
                        null,
                        null,
                        null));

        verify(userSearchIndexPort).indexUser(userId);
        verify(userSearchIndexPort, never()).reindexAll();
    }

    @Test
    void executeReindexesByDepartmentWhenDepartmentScopeProvided() {
        UUID departmentId = UUID.randomUUID();

        useCase.execute(
                new UserSearchReindexUseCase.Command(
                        false, Set.of(), null, departmentId, null, null));

        verify(userSearchIndexPort).reindexByDepartmentId(departmentId);
    }

    @Test
    void executeReindexesAllWhenRequested() {
        useCase.execute(
                new UserSearchReindexUseCase.Command(true, Set.of(), null, null, null, null));

        verify(userSearchIndexPort).reindexAll();
        verify(userSearchIndexPort, never()).indexUser(org.mockito.ArgumentMatchers.any());
    }
}
