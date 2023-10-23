package com.exoreaction.xorcery.status.providers;

import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.status.spi.StatusProvider;
import org.jvnet.hk2.annotations.Service;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

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
