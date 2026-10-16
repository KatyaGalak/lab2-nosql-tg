package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.OrganizationDto;
import com.university.booking.ui.Buttons;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrgsCommand extends Command {

    private final BackendClient client;

    public OrgsCommand(BackendClient client) {
        super(CommandType.ORGS);
        this.client = client;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        List<OrganizationDto> organizations = client.getOrganizations();
        if (organizations.isEmpty()) {
            return new SendMessage(chatId, "Организаций пока нет. Заявки можно подавать лично.");
        }
        StringBuilder sb = new StringBuilder("Организации, от имени которых можно подать заявку:\n\n");
        for (OrganizationDto o : organizations) {
            sb.append("• ").append(o.getName())
              .append(" (").append(o.getType().getLabel()).append(")")
              .append(o.isActive() ? "" : " — неактивна").append("\n");
            if (o.getContact() != null && !o.getContact().isBlank()) {
                sb.append("  контакт: ").append(o.getContact()).append("\n");
            }
            if (o.getStats() != null) {
                sb.append("  заявок: ").append(o.getStats().getTotal())
                  .append(", одобрено: ").append(o.getStats().getApproved()).append("\n");
            }
        }
        sb.append("\nОрганизацию выбирают при создании заявки в /book.");
        return new SendMessage(chatId, sb.toString())
                .replyMarkup(Buttons.row(Buttons.button("Забронировать", "/book")));
    }
}