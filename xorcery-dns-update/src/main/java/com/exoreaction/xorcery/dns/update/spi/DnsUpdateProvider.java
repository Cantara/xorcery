package com.exoreaction.xorcery.dns.update.spi;

import org.xbill.DNS.Record;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface DnsUpdateProvider {
    CompletionStage<Void> updateDns(String zone, List<Record> dnsUpdates);
}
