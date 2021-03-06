package com.sflpro.notifier.api.facade.security;

import com.sflpro.notifier.services.notification.dto.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * Created by Hayk Mkrtchyan.
 * Date: 6/26/19
 * Time: 3:07 PM
 */
class DefaultNotificationCreationPermissionChecker implements NotificationCreationPermissionChecker {

    private static final Logger logger = LoggerFactory.getLogger(DefaultNotificationCreationPermissionChecker.class);

    private final PermissionChecker permissionChecker;
    private final PermissionNameResolver permissionNameResolver;

    DefaultNotificationCreationPermissionChecker(final PermissionChecker permissionChecker,
                                                 final PermissionNameResolver permissionNameResolver) {
        this.permissionChecker = permissionChecker;
        this.permissionNameResolver = permissionNameResolver;
    }

    @Override
    public <N extends NotificationDto<?>> boolean isNotificationCreationAllowed(final N notification) {
        Assert.notNull(notification, "Null was passed as an argument for parameter 'creationRequest'.");
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            throw new IllegalStateException("SecurityContext is missing.");
        }
        final Authentication authentication = context.getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("SecurityContext should have Authentication assigned.");
        }
        if (authentication instanceof PreAuthenticatedAuthenticationToken) {
            return isPermitted(notification, authentication.getPrincipal().toString());
        }
        throw new IllegalStateException("No access token was found associated with notification creation request.");
    }

    private <R extends NotificationDto<?>> boolean isPermitted(final R creationRequest, final String token) {
        final Optional<String> permissionName = permissionNameResolver.resolve(creationRequest);
        if (!permissionName.isPresent()) {
            logger.debug("No permission name was resolved, request not permitted.");
            return false;
        }
        return permissionName.filter(permission -> permissionChecker.isPermitted(permission, token))
                .isPresent();
    }

}
