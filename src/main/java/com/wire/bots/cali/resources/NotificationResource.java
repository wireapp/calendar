package com.wire.bots.cali.resources;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/notifications")
public class NotificationResource {
    private final ClientRepo repo;

    public NotificationResource(ClientRepo repo) {
        this.repo = repo;
    }

    @POST
    public Response notification(@HeaderParam("X-Goog-Channel-ID") final String bot,
                                 @HeaderParam("X-Goog-Resource-State") final String state,
                                 @HeaderParam("X-Goog-Resource-ID") final String resourceId) {

        try {

            Logger.info("notification: X-Goog-Channel-ID:%s X-Goog-Resource-State: %s X-Goog-Resource-ID: %s",
                    bot,
                    state,
                    resourceId);

            WireClient wireClient = repo.getWireClient(bot);

            if (wireClient == null) {
                Logger.info("NotificationResource: %s missing wire client", bot);
                return Response.
                        status(410).
                        build();
            }

            try {
//                Events changes = CalendarAPI.getChanges(bot, null);
//
//                for (Event change : changes.getItems()) {
//                    String message = String.format("Event: %s at: %s", change.getDescription(), change.getStart().getDate());
//                    wireClient.sendText(message);
//                }
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