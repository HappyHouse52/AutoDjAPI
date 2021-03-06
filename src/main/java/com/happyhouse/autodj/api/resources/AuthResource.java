package com.happyhouse.autodj.api.resources;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.happyhouse.autodj.api.representations.SpotifyCredentials;
import com.happyhouse.autodj.api.services.UserService;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.models.AuthorizationCodeCredentials;
import com.wrapper.spotify.models.User;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

  private final Api spotifyApi;
  private final UserService userService;

  @Inject
  public AuthResource(Api spotifyApi, UserService userService) {
    this.spotifyApi = spotifyApi;
    this.userService = userService;
  }

  @GET
  public SpotifyCredentials login(@QueryParam("code") String code,
                                  @QueryParam("state") String state) throws IOException, WebApiException {

    if (!Strings.isNullOrEmpty(code) && !Strings.isNullOrEmpty(state)) {
      return this.loginSuccess(code, state);
    } else {
      return this.getSpotifyAuth();
    }
  }

  private SpotifyCredentials getSpotifyAuth() {
    final List<String> scopes = Arrays.asList("user-read-private", "user-read-email");
    final String newState = "someExpectedStateString";
    return new SpotifyCredentials(this.spotifyApi.createAuthorizeURL(scopes, newState));
  }

  private SpotifyCredentials loginSuccess(String code, String state) throws IOException, WebApiException {
    AuthorizationCodeCredentials creds = spotifyApi.authorizationCodeGrant(code).build().get();
    this.spotifyApi.setAccessToken(creds.getAccessToken());
    this.spotifyApi.setRefreshToken(creds.getRefreshToken());

    User spotifyUser = this.spotifyApi.getMe().build().get();
    if (this.userService.findById(spotifyUser.getId()) == null) {
      this.userService.createUser(spotifyUser.getId());
    }

    return new SpotifyCredentials(creds.getAccessToken(), creds.getRefreshToken());
  }
}
