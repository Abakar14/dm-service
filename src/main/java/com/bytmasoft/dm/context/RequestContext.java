package com.bytmasoft.dm.context;

/**
 * RequestContext
 *
 * @author Mahamat Abakar
 * @since 05.01.26
 */

public final class RequestContext {

  private static final ThreadLocal<RequestContextData> CTX = new ThreadLocal<>();

  public static void set(RequestContextData data) {
    CTX.set(data);
  }

  public static RequestContextData get() {
    return CTX.get();
  }

  public static void clear() {
    CTX.remove();
  }
}
