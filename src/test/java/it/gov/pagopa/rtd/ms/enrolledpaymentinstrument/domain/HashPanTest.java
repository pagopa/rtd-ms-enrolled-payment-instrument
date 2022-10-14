package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
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

    @DisplayName("valid hashpan should be create")
    @Test
    void hashPanValidMustDoNothing() {
        final var hash = TestUtils.randomString(64);
        final var hashPan = HashPan.create(hash);

        assertEquals(hash, hashPan.getValue());
    }

    @DisplayName("invalid hashpan should throw illegal argument exception")
    @Test
    void hashPanInvalidMustThrowIllegalException() {
        final var hashes = List.of(
                TestUtils.randomString(63),
                TestUtils.randomString(10),
                TestUtils.randomString(1),
                TestUtils.randomString(0)
        );
        hashes.forEach(hash -> assertThrows(IllegalArgumentException.class, () -> HashPan.create(hash)));
    }

}
