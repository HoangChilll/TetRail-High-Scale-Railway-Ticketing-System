package com.hoang.exception;

public class TicketUnavailableException extends RuntimeException {

    public TicketUnavailableException() {
        super("Một số vé đã được giữ. Vui lòng chọn lại.");
    }
}
