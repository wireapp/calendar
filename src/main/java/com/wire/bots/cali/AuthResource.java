package com.wire.bots.cali;

import com.wire.bots.sdk.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/user/auth/google_oauth2/callback")
public class AuthResource {

    @GET
    public Response auth(@QueryParam("bot") String bot,
                         @QueryParam("code") String code) throws Exception {

        Logger.info("AuthResource: Bot: %s, code: %s",
                bot,
                code);

        CalendarAPI.processAuthCode(bot, code);

        return Response.
                ok("You can enjoy Cali now").
                status(200).
                build();
    }

}