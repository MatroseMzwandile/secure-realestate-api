package com.wtc.realestate;

import com.wtc.realestate.dao.ListingDao;
import com.wtc.realestate.dao.RealtorDao;
import com.wtc.realestate.model.Listing;
import com.wtc.realestate.model.Realtor;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main {

    private static final RealtorDao realtorDao = new RealtorDao();
    private static final ListingDao listingDao = new ListingDao();


    private static final Map<String, Integer> sessions = new HashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(7000);


        app.get("/listings", ctx -> {
            ctx.json(listingDao.findAll());
        });

        app.get("/listings/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Listing listing = listingDao.findById(id);
            if (listing == null) {
                ctx.status(404).json(Map.of("error", "Listing not found"));
            } else {
                ctx.json(listing);
            }
        });


        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            Realtor realtor = realtorDao.login(username, password);
            if (realtor == null) {
                ctx.status(401).json(Map.of("error", "Invalid credentials"));
                return;
            }

            String token = UUID.randomUUID().toString();
            sessions.put(token, realtor.getId());
            ctx.json(Map.of("token", token, "realtorId", realtor.getId()));
        });


        app.post("/listings", ctx -> {
            Integer realtorId = requireAuth(ctx);
            if (realtorId == null) return;

            Listing listing = new Listing();
            listing.setTitle(ctx.formParam("title"));
            listing.setDescription(ctx.formParam("description"));
            listing.setPrice(new java.math.BigDecimal(ctx.formParam("price")));
            listing.setRealtorId(realtorId);

            listingDao.create(listing);
            ctx.status(201).json(Map.of("message", "Listing created"));
        });

        app.put("/listings/{id}", ctx -> {
            Integer realtorId = requireAuth(ctx);
            if (realtorId == null) return;

            Listing listing = new Listing();
            listing.setId(Integer.parseInt(ctx.pathParam("id")));
            listing.setTitle(ctx.formParam("title"));
            listing.setDescription(ctx.formParam("description"));
            listing.setPrice(new java.math.BigDecimal(ctx.formParam("price")));

            boolean updated = listingDao.update(listing);
            if (updated) {
                ctx.json(Map.of("message", "Listing updated"));
            } else {
                ctx.status(404).json(Map.of("error", "Listing not found"));
            }
        });

        app.delete("/listings/{id}", ctx -> {
            Integer realtorId = requireAuth(ctx);
            if (realtorId == null) return;

            // Same intentional gap as update() above — fixed in Phase 4.
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean deleted = listingDao.delete(id);
            if (deleted) {
                ctx.json(Map.of("message", "Listing deleted"));
            } else {
                ctx.status(404).json(Map.of("error", "Listing not found"));
            }
        });
    }


    private static Integer requireAuth(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null || !sessions.containsKey(token)) {
            ctx.status(401).json(Map.of("error", "Not logged in"));
            return null;
        }
        return sessions.get(token);
    }
}
