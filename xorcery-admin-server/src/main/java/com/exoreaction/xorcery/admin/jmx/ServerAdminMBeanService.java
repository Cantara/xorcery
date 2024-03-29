/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.admin.jmx;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.*;
import java.lang.management.ManagementFactory;

@Service(name="serveradmin")
@RunLevel(20)
public class ServerAdminMBeanService
    implements PreDestroy
{

    private final ServerMXBean.Model serverMXBean;
    private final ObjectName objectName;
    private final Logger logger;

    @Inject
    public ServerAdminMBeanService(Xorcery xorcery, Configuration configuration, Logger logger) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        this.logger = logger;
        serverMXBean = new ServerMXBean.Model(configuration, xorcery);
        objectName = ObjectName.getInstance("xorcery:name=admin");
        ManagementFactory.getPlatformMBeanServer().registerMBean(serverMXBean, objectName);
    }

    @Override
    public void preDestroy() {
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
        } catch (InstanceNotFoundException e) {
            // IGnore
        } catch (MBeanRegistrationException e) {
            logger.warn("Could not deregister admin MBean");
        }
    }
}
