package com.university.booking.service.dialog;

import com.university.booking.state.Context;
import com.university.booking.state.State;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StateService {

    private final Map<Long, Context> states = new ConcurrentHashMap<>();

    public Context get(long chatId) {
        return states.computeIfAbsent(chatId, k -> new Context());
    }

    public boolean isInDialog(long chatId) {
        return get(chatId).getState() != State.DEFAULT;
    }

    public void updateContext(long chatId, Consumer<Context> action) {
        states.compute(chatId, (k, ctx) -> {
            Context c = ctx == null ? new Context() : ctx;
            action.accept(c);
            return c;
        });
    }

    public void reset(long chatId) {
        Context ctx = states.get(chatId);
        if (ctx != null) {
            ctx.setState(State.DEFAULT);
            ctx.resetBooking();
            ctx.resetRegister();
            log.atInfo().addKeyValue("event", "state_reset").log("state reset for chatId {}", chatId);
        }
    }
}
