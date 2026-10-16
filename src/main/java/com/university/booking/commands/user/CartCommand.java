package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.CartDto;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.ui.Buttons;
import org.springframework.stereotype.Component;

@Component
public class CartCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;

    public CartCommand(BackendClient client, StateService stateService) {
        super(CommandType.CART);
        this.client = client;
        this.stateService = stateService;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        if (stateService.get(chatId).getPersonId() == null) throw new NotRegisteredException();

        CartDto cart = client.getCart();
        if (cart.getItems().isEmpty()) {
            return new SendMessage(chatId, "Корзина пуста (или время её хранения истекло)")
                    .replyMarkup(Buttons.row(Buttons.button("Добавить заявку", "/book")));
        }
        return new SendMessage(chatId, CartFormatter.formatCart(cart)).replyMarkup(CartFormatter.cartActions());
    }
}
