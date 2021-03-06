package com.sflpro.notifier.spi.push;


import org.springframework.util.Assert;

import java.util.Map;

/**
 * Created by Hayk Mkrtchyan.
 * Date: 7/3/19
 * Time: 10:43 AM
 */
public interface PushMessage {

    String destinationRouteToken();

    String subject();

    String body();

    PlatformType platformType();

    Map<String, String> properties();

    static PushMessage of(final String destinationRouteToken,
                          final String subject,
                          final String body,
                          final PlatformType platformType,
                          final Map<String, String> properties) {
        Assert.hasText(destinationRouteToken, "Null or empty text was passed as an argument for parameter 'destinationRouteToken'.");
        Assert.hasText(subject, "Null or empty text was passed as an argument for parameter 'subject'.");
        Assert.hasText(body, "Null or empty text was passed as an argument for parameter 'body'.");
        Assert.notNull(platformType, "Null was passed as an argument for parameter 'platformType'.");
        Assert.notNull(properties, "Null was passed as an argument for parameter 'properties'.");
        return new ImmutablePushMessage(destinationRouteToken, subject, body, platformType, properties);
    }
}
