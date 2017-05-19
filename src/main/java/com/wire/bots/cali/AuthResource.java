package com.wire.bots.cali;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Path("/user/auth/google_oauth2/callback")
public class AuthResource {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);

    @GET
    public Response auth(@QueryParam("state") final String bot,
                         @QueryParam("code") final String code) throws Exception {


        CalendarAPI.processAuthCode(bot, code);

        return Response.
                ok("Thank you! You can enjoy Cali now. Type: /list").
                status(200).
                build();
    }
}