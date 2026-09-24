package com.vita.redis.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedissonConfigSerializationTest {

    @Test
    void shouldRoundTripJavaTimeRecordAndCalendarList() {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(new RedissonConfig().redisObjectMapper());
        CalendarPayload source = new CalendarPayload(
                List.of(LocalDate.of(2026, 7, 28)),
                LocalDateTime.of(2026, 7, 28, 15, 0));

        Object restored = serializer.deserialize(serializer.serialize(source));

        assertThat(restored).isEqualTo(source);
    }

    private record CalendarPayload(List<LocalDate> dates, LocalDateTime sourceDataTime) {
    }
}
