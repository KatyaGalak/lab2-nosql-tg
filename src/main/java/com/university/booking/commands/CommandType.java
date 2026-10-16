package com.university.booking.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommandType {
    START("/start", "начало работы с ботом", false),
    HELP("/help", "список всех команд", false),
    REGISTER("/register", "зарегистрироваться", false),
    LOGIN("/login", "войти по номеру ИСУ: /login <ИСУ>", false),
    ROOMS("/rooms", "список доступных комнат", false),
    ROOM("/room", "комната и её занятость: /room <id> [ГГГГ-ММ-ДД]", false),
    BOOKINGS("/bookings", "мои заявки (администратор видит все)", false),
    BOOKING("/booking", "детали бронирования: /booking <id>", false),
    BOOK("/book", "добавить заявку в корзину (пошаговый диалог)", false),
    CART("/cart", "временная корзина заявок", false),
    CHECKOUT("/checkout", "отправить заявки из корзины на рассмотрение", false),
    CLEARCART("/clearcart", "очистить корзину", false),
    SUBMIT("/submit", "отправить черновик на рассмотрение: /submit <id>", false),
    CANCEL("/cancel", "отменить действие или удалить свою заявку: /cancel <id>", false),
    APPROVE("/approve", "одобрить бронирование: /approve <id>", true),
    REJECT("/reject", "отклонить бронирование: /reject <id>", true),
    PERSONS("/persons", "список пользователей", true),
    ADDPERSON("/addperson", "добавить пользователя: /addperson <имя> <роль>", true);

    private final String commandName;
    private final String description;
    private final boolean adminOnly;
}
