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
package dev.xorcery.admin.jmx;

import com.sun.management.HotSpotDiagnosticMXBean;
import dev.xorcery.configuration.ApplicationConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.core.Xorcery;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public interface ServerMXBean {

    public record Model(Configuration configuration, Xorcery xorcery) implements ServerMXBean {

        @Override
        public String getId() {
            return new InstanceConfiguration(configuration).getId();
        }

        @Override
        public String getName() {
            return new ApplicationConfiguration(configuration).getName();
        }

        @Override
        public void shutdown() {
            xorcery.close();
            System.exit(0);
        }

        @Override
        public String heapDump() throws IOException {
            HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
                    "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);

            String dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss").format(Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime());
            File heapDumpFile = new File(new File(InstanceConfiguration.get(configuration).getHome()), "heapdump-"+dateTime+".hprof");
            try {
                mxBean.dumpHeap(heapDumpFile.getAbsolutePath(), true);
                return "Heap dumped to:"+heapDumpFile;
            } catch (IOException e) {
                heapDumpFile.delete();
                throw e;
            }
        }
    }

    String getId();

    String getName();

    void shutdown();

    String heapDump() throws IOException;
}
