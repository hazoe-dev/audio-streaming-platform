package dev.hazoe.audiostreaming.audio.streaming;

import dev.hazoe.audiostreaming.common.exception.RangeNotSatisfiableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class RangeResolverTest {

    private static final long FILE_SIZE = 1_000L;
    private static final long CHUNK_SIZE = 100L;

    private RangeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RangeResolver(CHUNK_SIZE);
    }

    @Test
    void resolve_whenNoRangeHeader_returnsFullContent() {
        ByteRange range = resolver.resolve(null, FILE_SIZE);

        assertEquals(0, range.start());
        assertEquals(FILE_SIZE - 1, range.end());
        assertEquals(FILE_SIZE, range.contentLength());
        assertFalse(range.partial());
    }

    //Servers MUST ignore Range headers they do not understand.
    //Any header that is not bytes=... is treated as “no Range header”.
    @Test
    void resolve_whenInvalidPrefix_returnsFullContent() {
        ByteRange range = resolver.resolve("items=0-100", FILE_SIZE);

        assertEquals(0, range.start());
        assertEquals(FILE_SIZE - 1, range.end());
        assertEquals(FILE_SIZE, range.contentLength());
        assertFalse(range.partial());
    }

    @Test
    void resolve_whenBytes_500_999_shouldReturn500_999() {
        ByteRange range = resolver.resolve("bytes=500-999", FILE_SIZE);

        assertEquals(500, range.start());
        assertEquals(999, range.end());
        assertEquals(500, range.contentLength());
        assertTrue(range.partial());
    }

    @Test
    void resolve_whenBytes_500_dash_usesChunkSize_shouldReturn500_599() {
        ByteRange range = resolver.resolve("bytes=500-", FILE_SIZE);

        assertEquals(500, range.start());
        assertEquals(599, range.end()); // 500 + 100 - 1
        assertEquals(100, range.contentLength());
        assertTrue(range.partial());
    }

    @Test
    void resolve_whenChunkExceedsFileSize_shouldReturnBytes_950_dash_capsAtFileEnd() {
        ByteRange range = resolver.resolve("bytes=950-", FILE_SIZE);

        assertEquals(950, range.start());
        assertEquals(999, range.end());
        assertEquals(50, range.contentLength());
        assertTrue(range.partial());
    }

    @Test
    void resolve_whenSuffixRange_shouldReturnBytes_dash_200_returnsLast200Bytes() {
        ByteRange range = resolver.resolve("bytes=-200", FILE_SIZE);

        assertEquals(800, range.start());
        assertEquals(999, range.end());
        assertEquals(200, range.contentLength());
        assertTrue(range.partial());
    }

    @Test
    void resolve_whenNonNumericRange_throwsException() {
        assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=abc-def", FILE_SIZE)
        );
    }

    @Test
    void resolve_whenDecimalRange_throwsException() {
        assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=1.5-10", FILE_SIZE)
        );
    }

    @Test
    void resolve_whenStartGreaterThanEnd_throwsException() {
        assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=800-700", FILE_SIZE)
        );
    }

    @Test
    void resolve_whenStartBeyondFileSize_throwsException() {
        assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=1000-1100", FILE_SIZE)
        );
    }

    @Test
    void resolve_whenEndBeyondFileSize_throwsException() {
        assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=900-1000", FILE_SIZE)
        );
    }

    @Test
    void resolve_whenBytes_dash_zero_isInvalid_throwsException() {
        assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=-0", FILE_SIZE)
        );
    }

    @Test
    void resolve_whenBytes_dash_negative_isInvalid_throwsException() {
        assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=--100", FILE_SIZE)
        );
    }

    @Test
    void resolve_whenExceptionContainsFileSize_throwsException() {
        RangeNotSatisfiableException ex = assertThrows(
                RangeNotSatisfiableException.class,
                () -> resolver.resolve("bytes=abc", FILE_SIZE)
        );

        assertEquals(FILE_SIZE, ex.getFileSize());
    }

}
