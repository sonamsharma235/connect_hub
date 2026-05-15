package com.connecthub.auth.controller;

import com.connecthub.auth.service.FavouriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/me/favourites")
public class FavouriteController {

    private final FavouriteService favouriteService;

    public FavouriteController(FavouriteService favouriteService) {
        this.favouriteService = favouriteService;
    }

    @GetMapping
    public List<String> list(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        return favouriteService.list(principal.getUsername());
    }

    @PostMapping("/{roomCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable String roomCode,
                    @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        favouriteService.add(principal.getUsername(), roomCode);
    }

    @DeleteMapping("/{roomCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String roomCode,
                       @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        favouriteService.remove(principal.getUsername(), roomCode);
    }
}

