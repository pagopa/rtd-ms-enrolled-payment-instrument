package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class HashPanTest {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final Random random = new Random();

    @DisplayName("valid hashpan should be create")
    @Test
    void hashPanValidMustDoNothing() {
        final var hash = randomString(64);
        final var hashPan = HashPan.create(hash);

        assertEquals(hash, hashPan.getValue());
    }

    @DisplayName("invalid hashpan should throw illegal argument exception")
    @Test
    void hashPanInvalidMustThrowIllegalException() {
        final var hashes = List.of(
                randomString(63),
                randomString(10),
                randomString(1),
                randomString(0)
        );
        hashes.forEach(hash -> assertThrows(IllegalArgumentException.class, () -> HashPan.create(hash)));
    }

    private String randomString(int length) {
        return IntStream.range(0, length)
                .mapToObj(i -> "" + ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())))
                .collect(Collectors.joining(""));
    }
}
