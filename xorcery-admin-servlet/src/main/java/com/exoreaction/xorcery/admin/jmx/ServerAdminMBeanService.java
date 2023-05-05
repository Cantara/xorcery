package com.exoreaction.xorcery.admin.jmx;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.*;
import java.lang.management.ManagementFactory;

@Service
@RunLevel(20)
public class ServerAdminMBeanService {

    @Inject
    public ServerAdminMBeanService(Xorcery xorcery, Configuration configuration) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(new ServerMXBean.Model(configuration, xorcery), ObjectName.getInstance("xorcery:name=admin"));
    }
}
