/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.connector;

import javax.servlet.AsyncContext;

import org.apache.tomcat.util.net.SocketStatus;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * This class handles async processing.
 * 
 * #TomcatServletRequestListener is not called by Tomcat if async is started, so must finish the transaction by calling
 * {@link Transaction#requestDestroyed()}.
 */
@Weave
public abstract class CoyoteAdapter {

    /**
     * This method is called on an async dispatch.
     */
    public boolean asyncDispatch(org.apache.coyote.Request req, org.apache.coyote.Response res, SocketStatus status) {

        Request request = (Request) req.getNote(1);
        AsyncContext asyncContext = request.getAsyncContext();
        AgentBridge.asyncApi.resumeAsync(asyncContext);

        boolean result = Weaver.callOriginal();

        checkSuspend(req);

        return result;

    }

    /**
     * This method is called on the initial request.
     */
    public void service(org.apache.coyote.Request req, org.apache.coyote.Response res) {

        Weaver.callOriginal();

        checkSuspend(req);

    }

    private void checkSuspend(org.apache.coyote.Request req) {
        Request request = (Request) req.getNote(1);
        if (request.isAsyncStarted()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
                AgentBridge.getAgent().getTransaction().requestDestroyed();
            }
        }
    }

}
