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

import dev.xorcery.configuration.Configuration;
import dev.xorcery.jsonapi.Attributes;
import dev.xorcery.status.spi.StatusProvider;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.function.Predicate;

@Service(name="status.threads")
public class ThreadDumpStatusProvider
        implements StatusProvider {

    private final Integer maxFrames;

    @Inject
    public ThreadDumpStatusProvider(Configuration configuration) {
        maxFrames = configuration.getInteger("status.threads.maxFrames").orElse(30);
    }

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

    public String toString(ThreadInfo info ) {
        StringBuilder sb = new StringBuilder("\"" + info.getThreadName() + "\"" +
                (info.isDaemon() ? " daemon" : "") +
                " prio=" + info.getPriority() +
                " Id=" + info.getThreadId() + " " +
                info.getThreadState());
        if (info.getLockName() != null) {
            sb.append(" on " + info.getLockName());
        }
        if (info.getLockOwnerName() != null) {
            sb.append(" owned by \"" + info.getLockOwnerName() +
                    "\" Id=" + info.getLockOwnerId());
        }
        if (info.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (info.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');
        int i = 0;
        for (; i < info.getStackTrace().length && i < maxFrames; i++) {
            StackTraceElement ste = info.getStackTrace()[i];
            sb.append("\tat " + ste.toString());
            sb.append('\n');
            if (i == 0 && info.getLockInfo() != null) {
                Thread.State ts = info.getThreadState();
                switch (ts) {
                    case BLOCKED:
                        sb.append("\t-  blocked on " + info.getLockInfo());
                        sb.append('\n');
                        break;
                    case WAITING:
                        sb.append("\t-  waiting on " + info.getLockInfo());
                        sb.append('\n');
                        break;
                    case TIMED_WAITING:
                        sb.append("\t-  waiting on " + info.getLockInfo());
                        sb.append('\n');
                        break;
                    default:
                }
            }

            for (MonitorInfo mi : info.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked " + mi);
                    sb.append('\n');
                }
            }
        }
        if (i < info.getStackTrace().length) {
            sb.append("\t...");
            sb.append('\n');
        }

        LockInfo[] locks = info.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = " + locks.length);
            sb.append('\n');
            for (LockInfo li : locks) {
                sb.append("\t- " + li);
                sb.append('\n');
            }
        }
        sb.append('\n');
        return sb.toString();
    }
}
