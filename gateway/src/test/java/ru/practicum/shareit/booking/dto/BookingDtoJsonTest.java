package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {
    private static final LocalDateTime START = LocalDateTime.of(2027, 1, 10, 12, 30);
    private static final LocalDateTime END = START.plusHours(1);

    @Autowired
    private JacksonTester<BookingDto> json;

    @Test
    void writesIsoLocalDatesAndExcludesValidationProperty() throws Exception {
        assertThat(json.write(new BookingDto(5L, START, END))).isStrictlyEqualToJson(
                "{\"itemId\":5,\"start\":\"2027-01-10T12:30:00\",\"end\":\"2027-01-10T13:30:00\"}");
    }

    @Test
    void parsesIsoLocalDatesAndDerivesDateOrderFromInput() throws Exception {
        BookingDto dto = json.parseObject("{\"itemId\":5,\"start\":\"2027-01-10T13:30:00\","
                + "\"end\":\"2027-01-10T12:30:00\",\"periodValid\":true}");

        assertThat(dto).isEqualTo(new BookingDto(5L, END, START));
        assertThat(dto.isPeriodValid()).isFalse();
    }
}
