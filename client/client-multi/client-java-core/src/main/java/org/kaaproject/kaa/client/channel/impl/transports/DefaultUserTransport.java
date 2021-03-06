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

package org.kaaproject.kaa.client.channel.impl.transports;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.kaaproject.kaa.client.channel.UserTransport;
import org.kaaproject.kaa.client.event.EndpointAccessToken;
import org.kaaproject.kaa.client.event.EndpointKeyHash;
import org.kaaproject.kaa.client.event.registration.EndpointRegistrationProcessor;
import org.kaaproject.kaa.common.TransportType;
import org.kaaproject.kaa.common.endpoint.gen.EndpointAttachRequest;
import org.kaaproject.kaa.common.endpoint.gen.EndpointAttachResponse;
import org.kaaproject.kaa.common.endpoint.gen.EndpointDetachRequest;
import org.kaaproject.kaa.common.endpoint.gen.EndpointDetachResponse;
import org.kaaproject.kaa.common.endpoint.gen.SyncResponseResultType;
import org.kaaproject.kaa.common.endpoint.gen.UserSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.UserSyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultUserTransport extends AbstractKaaTransport implements
        UserTransport {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserTransport.class);

    private EndpointRegistrationProcessor processor;
    private final Map<EndpointAccessToken, EndpointKeyHash> attachedEndpoints = new HashMap<EndpointAccessToken, EndpointKeyHash>();

    @Override
    public UserSyncRequest createUserRequest() {
        if (processor != null) {
            UserSyncRequest request = new UserSyncRequest();

            Map<Integer, EndpointAccessToken> attachEndpointRequests = processor.getAttachEndpointRequests();
            List<EndpointAttachRequest> attachEPRequestList = new LinkedList<EndpointAttachRequest>();
            for (Map.Entry<Integer, EndpointAccessToken> attachEPRequest : attachEndpointRequests.entrySet()) {
                attachEPRequestList.add(new EndpointAttachRequest(attachEPRequest.getKey(), attachEPRequest.getValue().getToken()));
            }

            Map<Integer, EndpointKeyHash> detachEndpointRequests = processor.getDetachEndpointRequests();
            List<EndpointDetachRequest> detachEPRequestList = new LinkedList<EndpointDetachRequest>();
            for (Map.Entry<Integer, EndpointKeyHash> detachEPRequest : detachEndpointRequests.entrySet()) {
                detachEPRequestList.add(new EndpointDetachRequest(detachEPRequest.getKey(), detachEPRequest.getValue().getKeyHash()));
            }

            request.setEndpointAttachRequests(attachEPRequestList);
            request.setEndpointDetachRequests(detachEPRequestList);
            request.setUserAttachRequest(processor.getUserAttachRequest());
            return request;
        }
        return null;
    }

    @Override
    public void onUserResponse(UserSyncResponse response) throws IOException {
        if (processor != null) {
            boolean hasChanges = false;
            Map<Integer, EndpointAccessToken> attachEndpointRequests = processor.getAttachEndpointRequests();
            if (response.getEndpointAttachResponses() != null && !response.getEndpointAttachResponses().isEmpty()) {
                synchronized (attachEndpointRequests) {
                    for (EndpointAttachResponse attached : response.getEndpointAttachResponses()) {
                        EndpointAccessToken attachedToken = attachEndpointRequests.remove(attached.getRequestId());
                        if (attached.getResult() == SyncResponseResultType.SUCCESS) {
                            LOG.info("Token {}", attachedToken);
                            attachedEndpoints.put(attachedToken, new EndpointKeyHash(attached.getEndpointKeyHash()));
                            hasChanges = true;
                        } else {
                            LOG.error("Failed to attach endpoint. Attach endpoint request id: {}", attached.getRequestId());
                        }
                    }
                }
            }
            Map<Integer, EndpointKeyHash> detachEndpointRequests = processor.getDetachEndpointRequests();
            if (response.getEndpointDetachResponses() != null && !response.getEndpointDetachResponses().isEmpty()) {
                synchronized (detachEndpointRequests) {
                    for (EndpointDetachResponse detached : response.getEndpointDetachResponses()) {
                        EndpointKeyHash detachedEndpointKeyHash = detachEndpointRequests.remove(detached.getRequestId());
                        if (detached.getResult() == SyncResponseResultType.SUCCESS) {
                            if (detachedEndpointKeyHash != null) {
                                for (Map.Entry<EndpointAccessToken, EndpointKeyHash> entry : attachedEndpoints.entrySet()) {
                                    if (detachedEndpointKeyHash.equals(entry.getValue())) {
                                        EndpointKeyHash removed = attachedEndpoints.remove(entry.getKey());
                                        if (!hasChanges) {
                                            hasChanges = (removed != null);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            LOG.error("Failed to detach endpoint. Detach endpoint request id: {}", detached.getRequestId());
                        }
                    }
                }
            }

            if (hasChanges && clientState != null) {
                clientState.setAttachedEndpointsList(attachedEndpoints);
            }
            processor.onUpdate(response.getEndpointAttachResponses()
                    , response.getEndpointDetachResponses()
                    , response.getUserAttachResponse()
                    , response.getUserAttachNotification()
                    , response.getUserDetachNotification());
            LOG.info("Processed user response");
        }
    }

    @Override
    public void setEndpointRegistrationProcessor(EndpointRegistrationProcessor manager) {
        this.processor = manager;
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.USER;
    }

}
