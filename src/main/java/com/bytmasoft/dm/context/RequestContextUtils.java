package com.bytmasoft.dm.context;

/**
 * RequestContextUtils
 *
 * @author Mahamat Abakar
 * @since 05.01.26
 */

public final class RequestContextUtils {

  private RequestContextUtils() {
  }

  public static Long getCurrentUserId() {
    return RequestContext.get() != null ? RequestContext.get().userId() : null;
  }


  public static String getCurrentUsername() {
    return RequestContext.get() != null
        ? RequestContext.get().username()
        : null;
  }

  public static boolean hasRole(String role) {
    return RequestContext.get() != null &&
        RequestContext.get().roles().contains(role);
  }

  public static boolean hasPermission(String permission) {
    return RequestContext.get() != null &&
        RequestContext.get().permissions().contains(permission);
  }

  public static String getClientIp() {
    return RequestContext.get() != null
        ? RequestContext.get().clientIp()
        : null;
  }
}

