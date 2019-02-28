/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import static java.lang.Thread.currentThread;

import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;

/**
 * Handler for an incoming HTTP request that allows to send the HTTP response asynchronously.
 *
 * @since 4.0
 */
public interface RequestHandler {

  /**
   * Called to handle an incoming HTTP request
   *
   * @param requestContext HTTP request content
   * @param responseCallback callback to call when the response content is ready.
   */
  void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback);

  /**
   * Called to handle an incoming HTTP request and optionally push resources.
   *
   * @param requestContext HTTP request content
   * @param responseCallback callback to call when the response content is ready.
   * @param pushHandler HTTP server push handler
   * @since 4.2
   */
  default void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback,
                             PushHandler pushHandler) {
    handleRequest(requestContext, responseCallback, resource -> {
    });
  }

  /**
   * @return the classloader for the artifact that owns this {@link RequestHandler}.
   *
   * @since 4.1.5
   */
  default ClassLoader getContextClassLoader() {
    return currentThread().getContextClassLoader();
  }

}
