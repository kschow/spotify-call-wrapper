package com.wanderingmotivation.spotify.callwrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedAlbum;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedTrack;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.exceptions.BadRequestException;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.models.Album;
import com.wrapper.spotify.models.Artist;
import com.wrapper.spotify.models.AudioFeature;
import com.wrapper.spotify.models.ClientCredentials;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.SimpleAlbum;
import com.wrapper.spotify.models.SimpleTrack;
import com.wrapper.spotify.models.Track;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
public class SpotifyService {
    private static final Logger LOGGER = Logger.getLogger(SpotifyService.class);
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final Api spotifyApi;

    SpotifyService(@Value("${spotify.client.id}") final String clientId,
                   @Value("${spotify.client.secret}") final String clientSecret)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        spotifyApi = Api.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    public void getAuthToken() throws IOException, WebApiException, ExecutionException, InterruptedException {
        final ClientCredentials credentials = spotifyApi.clientCredentialsGrant().build().get();
        spotifyApi.setAccessToken(credentials.getAccessToken());
    }

    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    <T, R> Function<T, R> throwingFunctionWrapper(ThrowingFunction<T, R, Exception> throwingFunction) {
        return  i -> {
            try {
                return throwingFunction.apply(i);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private <T, K, F extends Function<K, T>> T getSpotifyObject(final K id,
                                                                final F spotifyApiRequest)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        T spotifyObject;
        try {
            spotifyObject = spotifyApiRequest.apply(id);
        } catch (final Exception e) {
            if (e.getCause() instanceof BadRequestException &&
                    e.getCause().getMessage().equals("401")) {
                // this occurs when the access token doesn't exist or expires
                LOGGER.info("bad access token, getting a new one");
                getAuthToken();
                spotifyObject = spotifyApiRequest.apply(id);
            } else {
                throw new RuntimeException(e);
            }
        }
        return spotifyObject;
    }

    @GetMapping("/search/artist")
    public List<WrappedArtist> searchForArtist(@RequestParam final String search)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        final Page<Artist> spotifyArtists = getSpotifyObject(search,
                throwingFunctionWrapper(s -> spotifyApi.searchArtists(s).build().get()));

        return spotifyArtists.getItems()
                .stream()
                .map(WrappedArtist::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/search/album")
    @ResponseBody
    public String searchForAlbum(@RequestParam final String search)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        final Page<SimpleAlbum> simpleAlbums = getSpotifyObject(search,
                throwingFunctionWrapper(s -> spotifyApi.searchAlbums(s).build().get()));
        final List<String> albumIds = simpleAlbums.getItems()
                .stream()
                .map(SimpleAlbum::getId)
                .collect(Collectors.toList());

        final List<Album> spotifyAlbums = getSpotifyObject(search,
                throwingFunctionWrapper(s -> spotifyApi.getAlbums(albumIds).build().get()));
        final List<WrappedAlbum> albums = spotifyAlbums
                .stream()
                .map(WrappedAlbum::new)
                .collect(Collectors.toList());

        return GSON.toJson(albums);
    }

    @GetMapping("/getArtistInfo/{artistId}")
    public Map<String, Object> getArtistInfo(@PathVariable final String artistId)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        final long startTime = System.currentTimeMillis();
        LOGGER.info("starting get info");

        final Artist spotifyArtist = getSpotifyObject(artistId,
                throwingFunctionWrapper(aid -> spotifyApi.getArtist(aid).build().get()));

        final Page<SimpleAlbum> spotifyArtistSimpleAlbums = getSpotifyObject(artistId,
                throwingFunctionWrapper(aid -> spotifyApi.getAlbumsForArtist(aid).build().get()));

        final Map<String, WrappedArtist> artists = new HashMap<>();
        artists.put(spotifyArtist.getId(), new WrappedArtist(spotifyArtist));
        final long artistTime = System.currentTimeMillis();
        LOGGER.info("got artist info, took: " + (artistTime - startTime) + "ms");

        final List<String> albumIds = spotifyArtistSimpleAlbums.getItems()
                .stream()
                .map(SimpleAlbum::getId)
                .collect(Collectors.toList());

        final List<Album> spotifyArtistAlbums = getSpotifyObject(albumIds,
                throwingFunctionWrapper(ids -> spotifyApi.getAlbums(ids).build().get()));

        final Map<String, WrappedAlbum> albums = spotifyArtistAlbums
                .stream()
                .map(WrappedAlbum::new)
                .collect(Collectors.toMap(WrappedAlbum::getSpotifyId, x -> x));

        final List<String> trackIds = spotifyArtistAlbums
                .stream()
                .map(Album::getTracks)
                .map(Page::getItems)
                .flatMap(Collection::stream)
                .map(SimpleTrack::getId)
                .collect(Collectors.toList());

        final long albumTime = System.currentTimeMillis();
        LOGGER.info("got album info, took: " + (albumTime - artistTime) + "ms");

        final List<WrappedTrack> tracks = getTracks(trackIds, artistId);

        final long trackTime = System.currentTimeMillis();
        LOGGER.info("got track info, took: " + (trackTime - albumTime) + "ms");

        final Map<String, Object> data = new HashMap<>();
        data.put("albums", albums);
        data.put("artists", artists);
        data.put("tracks", tracks);
        return data;
    }

    private List<WrappedTrack> getTracks(final List<String> trackIds, final String artistId)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        // temporary map for faster access during building of wrapped tracks
        final Map<String, WrappedTrack> tracks = new HashMap<>();

        /*
            This partitioning is done to make sure that requests are of valid length
            Since Spotify doesn't tell us exactly how long a Spotify ID is,
            I just checked a couple and they were around 20 characters
            By partitioning into chunks with 50 ids each, I'm giving it a bit of headroom
        */
        final List<List<String>> partitions = ListUtils.partition(trackIds, 50);
        for (final List<String> chunk : partitions) {
            final List<Track> spotifyTracks = getSpotifyObject(chunk,
                    throwingFunctionWrapper(ids -> spotifyApi.getTracks(ids).build().get()));
            final List<AudioFeature> spotifyAudioFeatures = getSpotifyObject(chunk,
                    throwingFunctionWrapper(ids -> spotifyApi.getAudioFeatures(ids).build().get()));

            for (final Track t : spotifyTracks) {
                tracks.put(t.getId(), new WrappedTrack(t));
            }
            for (final AudioFeature a : spotifyAudioFeatures) {
                /*
                    some tracks don't have audio features so their slot in the list is empty, hence the null check
                    this behavior of empty slots
                    appears to have to do with how the json is unmarshalled in the Java Spotify API
                */
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
                .filter(t -> t.getArtistIds().contains(artistId))
                .collect(Collectors.toList());
    }
}
