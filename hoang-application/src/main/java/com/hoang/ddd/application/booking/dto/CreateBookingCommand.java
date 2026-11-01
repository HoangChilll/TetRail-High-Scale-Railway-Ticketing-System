package com.hoang.ddd.application.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingCommand {

    @NotEmpty(message = "Danh sách vé không được để trống")
    private List<Long> ticketIds;

    @NotBlank(message = "Tên người đặt không được để trống")
    private String buyerName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String buyerEmail;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String buyerPhone;
}

