package com.bytmasoft.dm.context;

import java.util.Set;

/**
 * RequestContextData
 *
 * @author Mahamat Abakar
 * @since 05.01.26
 */
public record RequestContextData(
    Long userId,
    String username,
    Set<String> roles,
    Set<String> permissions,
    String clientIp,
    boolean trustedGateway
) {

}
