package com.wanderingmotivation.spotify.callwrapper.api.spotify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.AlbumSimplified;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.AudioFeatures;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Function;

import static com.wanderingmotivation.spotify.callwrapper.util.ThrowingFunctionWrappers.throwingFunctionWrapper;

@Component
@Slf4j
public class SpotifyApiWrapper {
    private final SpotifyApi spotifyApi;
    private final ClientCredentialsRequest clientCredentialsRequest;

    SpotifyApiWrapper(@Value("${spotify.client.id}") final String clientId,
                      @Value("${spotify.client.secret}") final String clientSecret) {
        spotifyApi = SpotifyApi.builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();
        clientCredentialsRequest = spotifyApi.clientCredentials().build();
    }

    private void getAuthToken() throws IOException, SpotifyWebApiException {
        final ClientCredentials credentials = clientCredentialsRequest.execute();
        spotifyApi.setAccessToken(credentials.getAccessToken());
        log.debug("Credentials expire in: " + credentials.getExpiresIn());
    }

    /**
     * Wraps the getting of a Spotify Object from the API
     * Mainly important to make sure a valid auth token exists and is available for requests
     * @param id Argument for spotifyApiRequest below
     * @param spotifyApiRequest The request function to get a Spotify object
     * @return The object gotten from a spotifyApiRequest
     */
    private <T, K, F extends Function<K, T>> T getSpotifyObjectFunction(final K id,
                                                                        final F spotifyApiRequest)
            throws SpotifyWebApiException, IOException {
        T spotifyObject;
        try {
            spotifyObject = spotifyApiRequest.apply(id);
        } catch (final Exception e) {
            if (e.getCause() instanceof UnauthorizedException) {
                // this occurs when the access token doesn't exist or expires
                log.debug("bad access token, getting a new one");
                getAuthToken();
                spotifyObject = spotifyApiRequest.apply(id);
            } else {
                throw e;
            }
        }
        return spotifyObject;
    }

    Paging<Artist> searchForArtist(final String search)
            throws SpotifyWebApiException, IOException {
        return getSpotifyObjectFunction(search,
                throwingFunctionWrapper(s -> spotifyApi.searchArtists(s).build().execute()));
    }

    Paging<AlbumSimplified> searchForAlbum(final String search)
            throws SpotifyWebApiException, IOException {
        return getSpotifyObjectFunction(search,
                throwingFunctionWrapper(s -> spotifyApi.searchAlbums(s).build().execute()));
    }

    Paging<PlaylistSimplified> searchForPlaylist(final String search)
            throws IOException, SpotifyWebApiException {
        return getSpotifyObjectFunction(search,
                throwingFunctionWrapper(s -> spotifyApi.searchPlaylists(s).build().execute()));
    }

    Album[] getSpotifyAlbums(final String[] albumIds) throws IOException, SpotifyWebApiException {
        return getSpotifyObjectFunction(albumIds,
                throwingFunctionWrapper(aids -> spotifyApi.getSeveralAlbums(aids).build().execute()));
    }

    Paging<AlbumSimplified> getSpotifyArtistsAlbums(final String artistId, final int offset)
            throws IOException, SpotifyWebApiException {
        return getSpotifyObjectFunction(artistId,
                throwingFunctionWrapper(s ->
                        spotifyApi.getArtistsAlbums(s)
                                .market(SpotifyApiConstants.US_MARKET)
                                .album_type("single,album")
                                .limit(SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE)
                                .offset(offset)
                                .build()
                                .execute()));
    }

    Paging<PlaylistTrack> getSpotifyPlaylistTracks(final String playlistId, final int offset)
            throws IOException, SpotifyWebApiException {
        return getSpotifyObjectFunction(playlistId,
                throwingFunctionWrapper(p -> spotifyApi.getPlaylistsTracks(p)
                        .market(SpotifyApiConstants.US_MARKET)
                        .limit(SpotifyApiConstants.PLAYLIST_TRACK_PAGE_SIZE)
                        .offset(offset)
                        .build()
                        .execute()));
    }

    Paging<TrackSimplified> getSpotifyAlbumTracks(final String albumId, final int offset)
            throws IOException, SpotifyWebApiException {
        return getSpotifyObjectFunction(albumId, throwingFunctionWrapper(s ->
                spotifyApi.getAlbumsTracks(s)
                        .market(SpotifyApiConstants.US_MARKET)
                        .limit(SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE)
                        .offset(offset)
                        .build()
                        .execute()));
    }

    Track[] getSpotifyTracks(final String[] trackIds) throws IOException, SpotifyWebApiException {
        return getSpotifyObjectFunction(trackIds,
                throwingFunctionWrapper(ids -> spotifyApi.getSeveralTracks(ids).build().execute()));
    }

    AudioFeatures[] getSpotifyAudioFeatures(final String[] trackIds) throws IOException, SpotifyWebApiException {
        return getSpotifyObjectFunction(trackIds,
                throwingFunctionWrapper(ids -> spotifyApi.getAudioFeaturesForSeveralTracks(ids).build().execute()));
    }
}
