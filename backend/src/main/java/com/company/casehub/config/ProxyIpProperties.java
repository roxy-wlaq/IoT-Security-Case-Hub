package com.company.casehub.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Network identities allowed to supply the proxy-generated client IP header.
 * The application must not trust X-Forwarded-For when a request arrived from
 * any other peer.
 */
@ConfigurationProperties(prefix = "casehub.security.proxy")
public class ProxyIpProperties {

    private List<String> trustedProxies = new ArrayList<>(List.of("127.0.0.1", "::1"));

    public List<String> getTrustedProxies() {
        return List.copyOf(trustedProxies);
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies == null ? new ArrayList<>() : new ArrayList<>(trustedProxies);
    }

    public boolean isTrustedProxy(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        final InetAddress remote;
        try {
            remote = InetAddress.getByName(remoteAddress.trim());
        } catch (UnknownHostException ex) {
            return false;
        }
        return trustedProxies.stream().anyMatch(entry -> contains(entry, remote));
    }

    private static boolean contains(String entry, InetAddress address) {
        String value = entry == null ? "" : entry.trim();
        if (value.isEmpty()) {
            return false;
        }
        int slash = value.indexOf('/');
        try {
            if (slash < 0) {
                return InetAddress.getByName(value).equals(address);
            }
            InetAddress network = InetAddress.getByName(value.substring(0, slash));
            int prefixLength = Integer.parseInt(value.substring(slash + 1));
            byte[] networkBytes = network.getAddress();
            byte[] addressBytes = address.getAddress();
            if (networkBytes.length != addressBytes.length
                    || prefixLength < 0 || prefixLength > networkBytes.length * 8) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != addressBytes[i]) {
                    return false;
                }
            }
            return remainingBits == 0
                    || (networkBytes[fullBytes] & (0xff << (8 - remainingBits)))
                    == (addressBytes[fullBytes] & (0xff << (8 - remainingBits)));
        } catch (UnknownHostException | NumberFormatException ex) {
            return false;
        }
    }
}
