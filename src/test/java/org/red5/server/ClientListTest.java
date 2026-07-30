package org.red5.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClientListTest {

    @Test
    void removeDeletesTheMatchedEntryImmediately() {
        ClientList<String> clients = new ClientList<>();
        clients.add("first");
        clients.add("second");

        assertThat(clients.remove("first")).isTrue();
        assertThat(clients).containsExactly("second");
        assertThat(clients.get(0)).isEqualTo("second");
    }
}
