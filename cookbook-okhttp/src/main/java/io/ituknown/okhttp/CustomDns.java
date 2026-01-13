package io.ituknown.okhttp;

import okhttp3.Dns;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class CustomDns implements Dns {
    private final List<InetAddress> dnsServers;

    public CustomDns(List<InetAddress> dnsServers) {
        this.dnsServers = dnsServers;
    }

    @NotNull
    @Override
    public List<InetAddress> lookup(@NotNull String hostname) throws UnknownHostException {
        if (dnsServers != null && !dnsServers.isEmpty()) {
            return dnsServers;
        }
        // 默认使用系统解析
        return Dns.SYSTEM.lookup(hostname);
    }
}