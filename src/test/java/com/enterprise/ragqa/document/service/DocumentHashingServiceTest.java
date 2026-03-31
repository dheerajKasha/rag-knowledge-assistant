package com.enterprise.ragqa.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentHashingServiceTest {

    private final DocumentHashingService documentHashingService = new DocumentHashingService();

    @Test
    void producesA64CharacterHexHash() {
        String hash = documentHashingService.sha256("hello world");
        assertThat(hash).hasSize(64).matches("[a-f0-9]+");
    }

    @Test
    void sameInputProducesSameHash() {
        String hash1 = documentHashingService.sha256("deterministic content");
        String hash2 = documentHashingService.sha256("deterministic content");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        String hash1 = documentHashingService.sha256("document A");
        String hash2 = documentHashingService.sha256("document B");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hashesEmptyString() {
        String hash = documentHashingService.sha256("");
        assertThat(hash).hasSize(64);
    }

    @Test
    void knownSha256Value() {
        // SHA-256 of "abc" is a well-known value
        String hash = documentHashingService.sha256("abc");
        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469f4f9e30b6de7a91c2");
    }
}
