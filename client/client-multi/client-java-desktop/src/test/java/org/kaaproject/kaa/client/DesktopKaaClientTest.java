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

package org.kaaproject.kaa.client;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.kaaproject.kaa.client.profile.ProfileContainer;
import org.kaaproject.kaa.schema.base.Profile;
import org.mockito.Mockito;

public class DesktopKaaClientTest {

    @Before
    public void cleanup() {
        File stateFile = new File("state.properties");
        if (stateFile.exists()) {
            stateFile.delete();
        }
    }

    @Test
    public void testClientInit() throws Exception {

        System.setProperty(KaaClientProperties.KAA_CLIENT_PROPERTIES_FILE, "client-test.properties");
        KaaClient clientSpy = Mockito.spy(Kaa.newClient(new DesktopKaaPlatformContext(), null));

        try {
            clientSpy.start();
        } finally {
            clientSpy.stop();
        }

    }

    @Test
    public void testClientStartBeforeInit() throws Exception {
        System.setProperty(KaaClientProperties.KAA_CLIENT_PROPERTIES_FILE, "client-test.properties");
        KaaClient clientSpy = Mockito.spy(Kaa.newClient(new DesktopKaaPlatformContext(), null));

        // does nothing before initialization;
        clientSpy.start();
    }

    @Test
    public void testClientStartPollAfterInit() throws Exception {

        System.setProperty(KaaClientProperties.KAA_CLIENT_PROPERTIES_FILE, "client-test.properties");

        KaaClient clientSpy = Mockito.spy(Kaa.newClient(new DesktopKaaPlatformContext(), null));
        ProfileContainer profileContainerMock = Mockito.mock(ProfileContainer.class);

        clientSpy.setProfileContainer(profileContainerMock);

        Mockito.when(profileContainerMock.getProfile()).thenReturn(new Profile());

        clientSpy.start();

        Thread.sleep(5000L);

        clientSpy.stop();
    }

}
