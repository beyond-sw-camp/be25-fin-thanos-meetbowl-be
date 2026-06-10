package com.meetbowl.application.auth;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.auth.LoginSession;
import com.meetbowl.domain.auth.LoginSessionRepositoryPort;

@Service
@Transactional
public class LogoutUseCase {

    private final LoginSessionRepositoryPort loginSessionRepositoryPort;

    public LogoutUseCase(LoginSessionRepositoryPort loginSessionRepositoryPort) {
        this.loginSessionRepositoryPort = loginSessionRepositoryPort;
    }

    public void execute(LogoutCommand command) {
        List<LoginSession> sessions =
                loginSessionRepositoryPort.findActiveByUserId(command.userId());
        sessions.forEach(session -> loginSessionRepositoryPort.save(session.deactivate()));
    }
}
