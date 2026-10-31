package org.afterglow.eventinventoryservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record CreateSeatsRequest(@NotEmpty List<String> seatIds, @NotNull @PositiveOrZero BigDecimal price) {}
