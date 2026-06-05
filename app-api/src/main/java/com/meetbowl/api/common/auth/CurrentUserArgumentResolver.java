package com.meetbowl.api.common.auth;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** request attribute 또는 Spring Security 인증 객체에 저장된 AuthenticatedUser를 @CurrentUser 파라미터로 전달한다. */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        CurrentUser currentUser = parameter.getParameterAnnotation(CurrentUser.class);
        Object value = findRequestAttributeUser(webRequest);

        if (value instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        AuthenticatedUser securityContextUser = findSecurityContextUser();
        if (securityContextUser != null) {
            return securityContextUser;
        }

        if (currentUser != null && !currentUser.required()) {
            return null;
        }

        if (value == null) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED);
        }

        throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "인증 사용자 정보가 올바르지 않습니다.");
    }

    private Object findRequestAttributeUser(NativeWebRequest webRequest) {
        return webRequest.getAttribute(
                AuthenticatedUserAttributes.CURRENT_USER, RequestAttributes.SCOPE_REQUEST);
    }

    private AuthenticatedUser findSecurityContextUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object details = authentication.getDetails();
        if (details instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        return null;
    }
}
