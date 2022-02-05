package com.wire.bots.cali.resources;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.wire.bots.cali.CalendarAPI;
import com.wire.lithium.ClientRepo;
import com.wire.xenon.WireClient;
import com.wire.xenon.assets.MessageText;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/notifications")
public class NotificationResource {
    private final ClientRepo repo;

    public NotificationResource(ClientRepo repo) {
        this.repo = repo;
    }

    @POST
    public Response notification(@HeaderParam("X-Goog-Channel-ID") final UUID bot,
                                 @HeaderParam("X-Goog-Resource-State") final String state,
                                 @HeaderParam("X-Goog-Resource-ID") final String resourceId) {

        try {

            Logger.info("notification: X-Goog-Channel-ID:%s X-Goog-Resource-State: %s X-Goog-Resource-ID: %s",
                    bot,
                    state,
                    resourceId);

            WireClient wireClient = repo.getClient(bot);

            if (wireClient == null) {
                Logger.info("NotificationResource: %s missing wire client", bot);
                CalendarAPI.stop(bot.toString(), resourceId);
                return Response.
                        status(410).
                        build();
            }

            try {
                Events changes = CalendarAPI.getChanges(bot.toString(), null);

                for (Event change : changes.getItems()) {
                    String message = String.format("Event: %s at: %s", change.getDescription(), change.getStart().getDate());
                    wireClient.send(new MessageText(message));
                }
            } catch (Exception e) {
                Logger.error("NotificationResource: %s %s", bot, e);
            }

            return Response.
                    status(200).
                    build();
        } catch (Exception e) {
            Logger.error("NotificationResource: %s %s", bot, e);
            //e.printStackTrace();
            return Response.
                    status(500).
                    build();
        }
    }
}
