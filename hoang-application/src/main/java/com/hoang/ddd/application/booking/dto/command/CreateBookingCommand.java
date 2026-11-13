package com.hoang.ddd.application.booking.dto.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingCommand {

    @NotEmpty(message = "Danh sách vé không được để trống")
    @Size(max = 5, message = "Mỗi booking được đặt tối đa 5 vé")
    private List<
            @NotNull(message = "ID vé không được null")
            @Positive(message = "ID vé phải lớn hơn 0")
                    Long> ticketIds;

    @NotBlank(message = "Tên người đặt không được để trống")
    private String buyerName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String buyerEmail;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String buyerPhone;

}

