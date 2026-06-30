package com.meetbowl.api.config;

import java.util.Arrays;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * meetbowl-be 역할 조건 구현이다.
 */
public class MeetbowlAppRoleCondition implements Condition {

    private static final String ROLE_PROPERTY = "meetbowl.app.role";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Object rawRoles =
                metadata.getAnnotationAttributes(ConditionalOnMeetbowlAppRole.class.getName())
                        .get("value");
        if (!(rawRoles instanceof MeetbowlAppRole[] allowedRoles)) {
            return false;
        }
        MeetbowlAppRole currentRole =
                MeetbowlAppRole.from(context.getEnvironment().getProperty(ROLE_PROPERTY));
        return Arrays.asList(allowedRoles).contains(currentRole);
    }
}
