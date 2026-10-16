package com.university.booking.service.dialog;

import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.commands.admin.ApproveCommand;
import com.university.booking.commands.admin.RejectCommand;
import com.university.booking.commands.user.CommentCommand;
import com.university.booking.state.Context;
import com.university.booking.state.State;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class BookingActionDialogHandler implements DialogStageHandler {

    public static final String NO_REASON = "без причины";
    public static final String NO_COMMENT = "без комментария";
    private static final int MAX_REASON_LENGTH = 500;
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final ApproveCommand approveCommand;
    private final RejectCommand rejectCommand;
    private final CommentCommand commentCommand;
    private final StateService stateService;

    public BookingActionDialogHandler(@Lazy ApproveCommand approveCommand,
                                      @Lazy RejectCommand rejectCommand,
                                      @Lazy CommentCommand commentCommand,
                                      StateService stateService) {
        this.approveCommand = approveCommand;
        this.rejectCommand = rejectCommand;
        this.commentCommand = commentCommand;
        this.stateService = stateService;
    }

    @Override
    public boolean supports(State state) {
        return state == State.APPROVE_COMMENT || state == State.REJECT_REASON || state == State.COMMENT_TEXT;
    }

    @Override
    public SendMessage handle(long chatId, String text) {
        Context ctx = stateService.get(chatId);
        String bookingId = ctx.getPendingBookingId();
        State state = ctx.getState();
        String value = text.trim();

        int maxLength = state == State.COMMENT_TEXT ? MAX_COMMENT_LENGTH : MAX_REASON_LENGTH;
        if (value.isEmpty()) {
            return new SendMessage(chatId, "Текст не может быть пустым. Попробуй ещё раз или /cancel:");
        }
        if (value.length() > maxLength) {
            return new SendMessage(chatId, "Слишком длинный текст (до " + maxLength + " символов). Сократи его:");
        }

        stateService.reset(chatId);
        return switch (state) {
            case APPROVE_COMMENT -> approveCommand.approve(chatId, bookingId,
                    NO_COMMENT.equalsIgnoreCase(value) ? null : value);
            case REJECT_REASON -> rejectCommand.reject(chatId, bookingId,
                    NO_REASON.equalsIgnoreCase(value) ? null : value);
            default -> commentCommand.comment(chatId, bookingId, value);
        };
    }
}
