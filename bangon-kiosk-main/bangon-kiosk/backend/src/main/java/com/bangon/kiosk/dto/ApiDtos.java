package com.bangon.kiosk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request bodies accepted by the REST API. */
public class ApiDtos {

    public record Register(@NotBlank String name,
                           @Email @NotBlank String email,
                           @Size(min = 8) String password) {
    }

    public record Login(@Email @NotBlank String email,
                        @NotBlank String password) {
    }

    public record ProfileUpdate(@Min(0) @Max(2000) int caffeineLimit,
                                @Min(0) @Max(20000) int calorieGoal,
                                boolean lactoseIntolerant) {
    }

    public record Choices(String size, String milk, Integer sweetness,
                          boolean extraShot, boolean caramel, boolean cream) {
    }

    public record Line(@NotNull Long productId,
                       @Min(1) @Max(30) int quantity,
                       Choices choices) {
    }

    public record Checkout(@NotEmpty List<@Valid Line> items,
                           @NotBlank String paymentMethod,
                           boolean redeemPoints,
                           @NotBlank String idempotencyKey) {
    }
}
