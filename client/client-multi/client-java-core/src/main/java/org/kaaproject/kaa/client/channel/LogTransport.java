/*
 * Copyright 2014 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaaproject.kaa.client.channel;

import org.kaaproject.kaa.client.logging.LogProcessor;
import org.kaaproject.kaa.common.endpoint.gen.LogSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.LogSyncResponse;

/**
 * {@link KaaTransport} for the Logging service.
 * It is responsible for sending logs to the remote server.
 *
 * @author Yaroslav Zeygerman
 *
 */
public interface LogTransport extends KaaTransport {

    /**
     * Creates the Log request. It consists of current log records.
     *
     * @return new Log request;
     *
     */
    LogSyncRequest createLogRequest();

    /**
     * Updates the state of the Log collector from the given response.
     *
     * @param response the response from the server.
     *
     */
    void onLogResponse(LogSyncResponse response);

    /**
     * Sets the given Log processor to the current transport.
     *
     * @param processor the Log processor which is going to be set.
     *
     */
    void setLogProcessor(LogProcessor processor);

}