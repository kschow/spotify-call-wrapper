package com.wanderingmotivation.spotify.callwrapper;

import com.neovisionaries.i18n.CountryCode;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.wanderingmotivation.spotify.callwrapper.util.ThrowingFunctionWrappers.throwingFunctionWrapper;

@RestController
@CrossOrigin
public class SpotifyService {
    private static final Logger LOGGER = Logger.getLogger(SpotifyService.class);
    private static final int ALBUM_PARTITION_SIZE = 20;
    private static final int TRACK_PARTITION_SIZE = 50;
    private static final int PAGE_SIZE = TRACK_PARTITION_SIZE;

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
                throwingFunctionWrapper(s -> spotifyApi.searchArtists(s).build().execute(), LOGGER));

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
                throwingFunctionWrapper(s -> spotifyApi.searchAlbums(s).build().execute(), LOGGER));
        final String[] albumIds = Arrays.stream(simpleAlbums.getItems())
                .map(AlbumSimplified::getId)
                .collect(Collectors.toList())
                .toArray(new String[] {});

        final Album[] spotifyAlbums = getSpotifyObject(albumIds,
                throwingFunctionWrapper(aids -> spotifyApi.getSeveralAlbums(aids).build().execute(), LOGGER));
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
                throwingFunctionWrapper(aid -> spotifyApi.getArtist(aid).build().execute(), LOGGER));

        final Map<String, WrappedArtist> artists = new HashMap<>();
        artists.put(spotifyArtist.getId(), new WrappedArtist(spotifyArtist));

        final long artistTime = System.currentTimeMillis();
        LOGGER.info("got artist info, took: " + (artistTime - startTime) + "ms");

        final Map<String, WrappedAlbum> albums = getManyAlbums(artistId);

        final List<String> albumIds = albums.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .map(WrappedAlbum::getSpotifyId)
                .collect(Collectors.toList());

        final long albumTime = System.currentTimeMillis();
        LOGGER.info("got album info, took: " + (albumTime - artistTime) + "ms");

        final List<String> trackIds = getAlbumTracks(albumIds);
        final Map<String, WrappedTrack> tracks = getManyTracks(trackIds, artistId);

        final long trackTime = System.currentTimeMillis();
        LOGGER.info("got track info, took: " + (trackTime - albumTime) + "ms");

        final Map<String, Object> data = new HashMap<>();
        data.put("albums", albums);
        data.put("artists", artists);
        data.put("tracks", tracks);
        return data;
    }

    private Map<String, WrappedAlbum> getManyAlbums(final String artistId) throws SpotifyWebApiException, IOException {
        final List<String> albumIds = new ArrayList<>();

        for (int i = 0; ; i += PAGE_SIZE) {
            final int offset = i;
            final Paging<AlbumSimplified> page = getSpotifyObject(artistId,
                    throwingFunctionWrapper(s ->
                            spotifyApi.getArtistsAlbums(s)
                                    .market(CountryCode.US)
                                    .album_type("single,album")
                                    .limit(PAGE_SIZE)
                                    .offset(offset)
                                    .build()
                                    .execute(), LOGGER));
            final int totalAlbums = page.getTotal();
            LOGGER.info(String.format("getting ids for albums %s to %s out of %s",
                    i, i + PAGE_SIZE, totalAlbums));
            albumIds.addAll(Arrays.stream(page.getItems())
                    .map(AlbumSimplified::getId)
                    .collect(Collectors.toList()));

            if (offset + PAGE_SIZE >= totalAlbums) {
                break;
            }
        }

        final Map<String, WrappedAlbum> albums = new HashMap<>();
        final List<List<String>> partitions = ListUtils.partition(albumIds, ALBUM_PARTITION_SIZE);
        for (final List<String> chunk : partitions) {
            final String[] chunkArray = chunk.toArray(new String[] {});
            final Album[] spotifyAlbums = getSpotifyObject(chunkArray,
                    throwingFunctionWrapper(ids -> spotifyApi.getSeveralAlbums(ids).build().execute(), LOGGER));
            for (final Album a : spotifyAlbums) {
                albums.put(a.getId(), new WrappedAlbum(a));
            }
        }
        return albums;
    }

    private List<String> getAlbumTracks(final List<String> albumIds) throws IOException, SpotifyWebApiException {
        final List<String> trackIds = new ArrayList<>();

        for (final String albumId : albumIds) {
            for (int i = 0; ; i += PAGE_SIZE) {
                final int offset = i;
                final Paging<TrackSimplified> page = getSpotifyObject(albumId,
                        throwingFunctionWrapper(s ->
                                spotifyApi.getAlbumsTracks(s)
                                        .market(CountryCode.US)
                                        .limit(PAGE_SIZE)
                                        .offset(offset)
                                        .build()
                                        .execute(), LOGGER));
                final int totalTracks = page.getTotal();
                LOGGER.info(String.format("getting ids for tracks %s to %s out of %s",
                        i, i + PAGE_SIZE, totalTracks));
                trackIds.addAll(Arrays.stream(page.getItems())
                        .map(TrackSimplified::getId)
                        .collect(Collectors.toList()));

                if (offset + PAGE_SIZE >= totalTracks) {
                    break;
                }
            }
        }
        return trackIds;
    }

    private Map<String, WrappedTrack> getManyTracks(final List<String> trackIds, final String artistId)
            throws SpotifyWebApiException, IOException {
        final Map<String, WrappedTrack> tracks = new HashMap<>();
        final List<List<String>> partitions = ListUtils.partition(trackIds, TRACK_PARTITION_SIZE);
        for (final List<String> chunk : partitions) {
            final String[] chunkArray = chunk.toArray(new String[] {});
            final Track[] spotifyTracks = getSpotifyObject(chunkArray,
                    throwingFunctionWrapper(ids -> spotifyApi.getSeveralTracks(ids).build().execute(), LOGGER));
            final AudioFeatures[] spotifyAudioFeatures = getSpotifyObject(chunkArray,
                    throwingFunctionWrapper(ids -> spotifyApi.getAudioFeaturesForSeveralTracks(ids).build().execute(),
                            LOGGER));

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
