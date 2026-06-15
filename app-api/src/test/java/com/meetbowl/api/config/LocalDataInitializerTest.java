package com.meetbowl.api.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import com.meetbowl.application.auth.InitializeLocalAccountsUseCase;

class LocalDataInitializerTest {

    @Test
    void skipsLocalAccountInitializationWhenSchemaIsNotReady() {
        InitializeLocalAccountsUseCase useCase = mock(InitializeLocalAccountsUseCase.class);
        doThrow(new InvalidDataAccessResourceUsageException("users table missing"))
                .when(useCase)
                .execute();

        LocalDataInitializer initializer = new LocalDataInitializer(useCase);

        assertDoesNotThrow(() -> initializer.run(null));
        verify(useCase).execute();
    }
}
