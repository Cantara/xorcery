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
package dev.xorcery.status.providers;

import dev.xorcery.jsonapi.Attributes;
import dev.xorcery.status.spi.StatusProvider;
import org.jvnet.hk2.annotations.Service;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.function.Predicate;

@Service(name="status.threads")
public class ThreadDumpStatusProvider
        implements StatusProvider {

    @Override
    public String getId() {
        return "threads";
    }

    @Override
    public void addAttributes(Attributes.Builder attrs, String include) {

        ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = bean.dumpAllThreads(true, true);

        String includeLower = include.toLowerCase();
        Predicate<ThreadInfo> filter = include.isBlank() ? t->true: t->t.getThreadName().toLowerCase().contains(includeLower);

        for (ThreadInfo info : infos) {
            if (filter.test(info))
                attrs.attribute(info.getThreadName(), info.toString());
        }

/*
        Map<Thread, StackTraceElement[]> threadDumps = Thread.getAllStackTraces();

        List<Thread> threads = new ArrayList<>(threadDumps.keySet());
        threads.sort(comparing(Thread::getName));
        for (Thread thread : threads) {
            StackTraceElement[] stackTraceElements = threadDumps.get(thread);
            StringBuilder threadDump = new StringBuilder();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                threadDump.append(stackTraceElement.toString()).append('\n');
            }
            attrs.attribute(thread.getName(), threadDump.toString());
        }
*/
    }
}
