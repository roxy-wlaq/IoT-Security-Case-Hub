package com.company.casehub.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProxyIpPropertiesTest {

    @Test
    void acceptsConfiguredProxyNetworks() {
        ProxyIpProperties properties = new ProxyIpProperties();
        properties.setTrustedProxies(List.of("127.0.0.1", "172.16.0.0/12"));

        assertThat(properties.isTrustedProxy("127.0.0.1")).isTrue();
        assertThat(properties.isTrustedProxy("172.20.0.4")).isTrue();
        assertThat(properties.isTrustedProxy("10.0.0.4")).isFalse();
    }

    @Test
    void rejectsUntrustedPeerEvenWhenItSuppliesForwardedHeader() {
        ProxyIpProperties properties = new ProxyIpProperties();
        properties.setTrustedProxies(List.of("127.0.0.1"));

        assertThat(properties.isTrustedProxy("203.0.113.10")).isFalse();
    }
}
