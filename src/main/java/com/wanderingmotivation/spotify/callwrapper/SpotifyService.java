package com.wanderingmotivation.spotify.callwrapper;

import com.wanderingmotivation.spotify.callwrapper.model.WrappedAlbum;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedTrack;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.AlbumSimplified;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.AudioFeatures;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
public class SpotifyService {
    private static final Logger LOGGER = Logger.getLogger(SpotifyService.class);
    private static final int TRACK_PARTITION_SIZE = 50;

    private final SpotifyApi spotifyApi;
    private final ClientCredentialsRequest clientCredentialsRequest;

    SpotifyService(@Value("${spotify.client.id}") final String clientId,
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
        LOGGER.info("Credentials expire in: " + credentials.getExpiresIn());
    }

    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    private <T, R> Function<T, R> throwingFunctionWrapper(ThrowingFunction<T, R, Exception> throwingFunction) {
        return  i -> {
            try {
                return throwingFunction.apply(i);
            } catch (final Exception e) {
                LOGGER.info(e, e);
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Wraps the getting of a Spotify Object from the API
     * Mainly important to make sure a valid auth token exists and is available for requests
     * @param id Argument for spotifyApiRequest below
     * @param spotifyApiRequest The request function to get a Spotify object
     * @return The object gotten from a spotifyApiRequest
     */
    private <T, K, F extends Function<K, T>> T getSpotifyObject(final K id,
                                                                final F spotifyApiRequest)
            throws SpotifyWebApiException, IOException {
        T spotifyObject;
        try {
            spotifyObject = spotifyApiRequest.apply(id);
        } catch (final Exception e) {
            if (e.getCause() instanceof UnauthorizedException) {
                // this occurs when the access token doesn't exist or expires
                LOGGER.info("bad access token, getting a new one");
                getAuthToken();
                spotifyObject = spotifyApiRequest.apply(id);
            } else {
                throw e;
            }
        }
        return spotifyObject;
    }

    /**
     * Searches for an artist
     * @param search search parameter matching spotify-web-api-java's SearchArtistsRequest
     * @return list of artists returned
     */
    @GetMapping("/search/artist")
    public List<WrappedArtist> searchForArtist(@RequestParam final String search)
            throws SpotifyWebApiException, IOException {
        final Paging<Artist> spotifyArtists = getSpotifyObject(search,
                throwingFunctionWrapper(s -> spotifyApi.searchArtists(s).build().execute()));

        return Arrays.stream(spotifyArtists.getItems())
                .map(WrappedArtist::new)
                .collect(Collectors.toList());
    }

    /**
     * Searches for an album
     * @param search search parameter matching spotify-web-api-java's SearchAlbumsRequest
     * @return list of albums returned
     */
    @GetMapping("/search/album")
    public List<WrappedAlbum> searchForAlbum(@RequestParam final String search)
            throws SpotifyWebApiException, IOException {
        final Paging<AlbumSimplified> simpleAlbums = getSpotifyObject(search,
                throwingFunctionWrapper(s -> spotifyApi.searchAlbums(s).build().execute()));
        final String[] albumIds = Arrays.stream(simpleAlbums.getItems())
                .map(AlbumSimplified::getId)
                .collect(Collectors.toList())
                .toArray(new String[] {});

        final Album[] spotifyAlbums = getSpotifyObject(albumIds,
                throwingFunctionWrapper(aids -> spotifyApi.getSeveralAlbums(aids).build().execute()));
        return Arrays.stream(spotifyAlbums)
                .map(WrappedAlbum::new)
                .collect(Collectors.toList());
    }

    /**
     * Get full artist information including:
     * All albums for an artist
     * All tracks for an artist
     * Full artist information
     * @param artistId Spotify URI for an artist
     * @return Map of artist, track, and album information
     */
    @GetMapping("/getArtistInfo/{artistId}")
    public Map<String, Object> getArtistInfo(@PathVariable final String artistId)
            throws SpotifyWebApiException, IOException {
        final long startTime = System.currentTimeMillis();
        LOGGER.info("starting get info");

        final Artist spotifyArtist = getSpotifyObject(artistId,
                throwingFunctionWrapper(aid -> spotifyApi.getArtist(aid).build().execute()));

        final Paging<AlbumSimplified> spotifyArtistSimpleAlbums = getSpotifyObject(artistId,
                throwingFunctionWrapper(aid -> spotifyApi.getArtistsAlbums(aid).build().execute()));

        final Map<String, WrappedArtist> artists = new HashMap<>();
        artists.put(spotifyArtist.getId(), new WrappedArtist(spotifyArtist));

        final long artistTime = System.currentTimeMillis();
        LOGGER.info("got artist info, took: " + (artistTime - startTime) + "ms");

        final String[] albumIds = Arrays.stream(spotifyArtistSimpleAlbums.getItems())
                .map(AlbumSimplified::getId)
                .collect(Collectors.toList())
                .toArray(new String[] {});

        final Album[] spotifyArtistAlbums = getSpotifyObject(albumIds,
                throwingFunctionWrapper(ids -> spotifyApi.getSeveralAlbums(ids).build().execute()));

        final Map<String, WrappedAlbum> albums = Arrays.stream(spotifyArtistAlbums)
                .map(WrappedAlbum::new)
                .collect(Collectors.toMap(WrappedAlbum::getSpotifyId, a -> a));

        final List<String> trackIds = Arrays.stream(spotifyArtistAlbums)
                .map(Album::getTracks)
                .map(Paging::getItems)
                .map(Arrays::asList)
                .flatMap(Collection::stream)
                .map(TrackSimplified::getId)
                .collect(Collectors.toList());

        final long albumTime = System.currentTimeMillis();
        LOGGER.info("got album info, took: " + (albumTime - artistTime) + "ms");

        final Map<String, WrappedTrack> tracks = getManyTracks(trackIds, artistId);

        final long trackTime = System.currentTimeMillis();
        LOGGER.info("got track info, took: " + (trackTime - albumTime) + "ms");

        final Map<String, Object> data = new HashMap<>();
        data.put("albums", albums);
        data.put("artists", artists);
        data.put("tracks", tracks);
        return data;
    }

    private Map<String, WrappedTrack> getManyTracks(final List<String> trackIds, final String artistId)
            throws SpotifyWebApiException, IOException {
        final Map<String, WrappedTrack> tracks = new HashMap<>();
        final List<List<String>> partitions = ListUtils.partition(trackIds, TRACK_PARTITION_SIZE);
        for (final List<String> chunk : partitions) {
            final String[] chunkArray = chunk.toArray(new String[] {});
            final Track[] spotifyTracks = getSpotifyObject(chunkArray,
                    throwingFunctionWrapper(ids -> spotifyApi.getSeveralTracks(ids).build().execute()));
            final AudioFeatures[] spotifyAudioFeatures = getSpotifyObject(chunkArray,
                    throwingFunctionWrapper(ids -> spotifyApi.getAudioFeaturesForSeveralTracks(ids).build().execute()));

            for (final Track t : spotifyTracks) {
                tracks.put(t.getId(), new WrappedTrack(t));
            }
            for (final AudioFeatures a : spotifyAudioFeatures) {
                // some tracks don't have audio features so their slot in the list is empty, hence the null check
                if (a != null && tracks.containsKey(a.getId())) {
                    final WrappedTrack t = tracks.get(a.getId());
                    t.setAudioFeatures(a);
                }
            }
        }

        return tracks.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                // remove tracks that might be on collaborative albums that don't include the artist
                // could do this earlier but the format of SimpleArtist makes it a bit more annoying
                .filter(t -> artistId == null || t.getArtistIds().contains(artistId))
                .collect(Collectors.toMap(WrappedTrack::getSpotifyId, t -> t));
    }
}
