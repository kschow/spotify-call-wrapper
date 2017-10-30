package com.wanderingmotivation.spotify.callwrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wanderingmotivation.spotify.callwrapper.model.FrontEndData;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedAlbum;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedTrack;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.exceptions.BadRequestException;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.models.*;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
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
    @ResponseBody
    public String searchForArtist(@RequestParam final String search)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        final Page<Artist> spotifyArtists = getSpotifyObject(search,
                throwingFunctionWrapper(s -> spotifyApi.searchArtists(s).build().get()));

        final List<WrappedArtist> artists = spotifyArtists.getItems()
                .stream()
                .map(WrappedArtist::new)
                .collect(Collectors.toList());
        return GSON.toJson(artists);
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

    @GetMapping("/getArtistInfo/{id}")
    @ResponseBody
    public String getArtistInfo(@PathVariable final String id)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
        final long startTime = System.currentTimeMillis();
        LOGGER.info("starting get info");

        final Artist spotifyArtist = getSpotifyObject(id,
                throwingFunctionWrapper(aid -> spotifyApi.getArtist(aid).build().get()));

        final Page<SimpleAlbum> spotifyArtistSimpleAlbums = getSpotifyObject(id,
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
                .collect(Collectors.toMap(WrappedAlbum::getAlbumId, x -> x));

        final List<String> trackIds = spotifyArtistAlbums
                .stream()
                .map(Album::getTracks)
                .map(Page::getItems)
                .flatMap(Collection::stream)
                .map(SimpleTrack::getId)
                .collect(Collectors.toList());

        final long albumTime = System.currentTimeMillis();
        LOGGER.info("got album info, took: " + (albumTime - artistTime) + "ms");

        final Map<String, WrappedTrack> tracks = getTracks(trackIds);

        final long trackTime = System.currentTimeMillis();
        LOGGER.info("got track info, took: " + (trackTime - albumTime) + "ms");

        final Map<String, Map> data = new HashMap<>();
        data.put("albums", albums);
        data.put("artists", artists);
        data.put("tracks", tracks);
        return GSON.toJson(new FrontEndData(data));
    }

    private Map<String, WrappedTrack> getTracks(final List<String> trackIds)
            throws WebApiException, ExecutionException, InterruptedException, IOException {
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
                if (a != null && tracks.containsKey(a.getId())) {
                    final WrappedTrack t = tracks.get(a.getId());
                    t.setAudioFeatures(a);
                }
            }
        }
        return tracks;
    }
}
