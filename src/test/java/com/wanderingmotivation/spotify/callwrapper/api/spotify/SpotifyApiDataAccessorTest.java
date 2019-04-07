package com.wanderingmotivation.spotify.callwrapper.api.spotify;

import com.wanderingmotivation.spotify.callwrapper.model.WrappedAlbum;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedPlaylist;
import com.wrapper.spotify.enums.AlbumType;
import com.wrapper.spotify.enums.ReleaseDatePrecision;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.AlbumSimplified;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Image;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import com.wrapper.spotify.model_objects.specification.User;
import org.apache.commons.collections4.ListUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpotifyApiDataAccessorTest {
    private SpotifyApiDataAccessor spotifyApiDataAccessor;
    @Mock private SpotifyApiWrapper mockSpotifyApiWrapper;

    @BeforeEach
    void initialize() {
        MockitoAnnotations.initMocks(this);
        spotifyApiDataAccessor = new SpotifyApiDataAccessor(mockSpotifyApiWrapper);
    }

    @Test
    @DisplayName("Base search for artist test")
    void searchForArtist() throws IOException, SpotifyWebApiException {
        final int testCount = 10;
        final String testSearchTerm = "search";

        final List<WrappedArtist> expectedArtists = buildWrappedArtistList(testCount);
        final Paging<Artist> mockArtistPage = buildArtistPage(testCount);
        when(mockSpotifyApiWrapper.searchForArtist(testSearchTerm)).thenReturn(mockArtistPage);

        assertEquals(expectedArtists, spotifyApiDataAccessor.searchForArtist(testSearchTerm));
    }

    @Test
    @DisplayName("Base search for album test")
    void searchForAlbum() throws IOException, SpotifyWebApiException {
        final int testCount = 10;
        final String testSearchTerm = "search";

        final List<WrappedAlbum> expectedAlbums = buildWrappedAlbumList(testCount);
        final Paging<AlbumSimplified> mockSimpleAlbumPage = buildSimplifiedAlbumPage(testCount, 0);
        final String[] mockAlbumIds = buildIdsArray(0, testCount);
        final Album[] mockAlbums = buildAlbums(testCount, 0);
        when(mockSpotifyApiWrapper.searchForAlbum(testSearchTerm)).thenReturn(mockSimpleAlbumPage);
        when(mockSpotifyApiWrapper.getSpotifyAlbums(mockAlbumIds)).thenReturn(mockAlbums);

        assertEquals(expectedAlbums, spotifyApiDataAccessor.searchForAlbum(testSearchTerm));
    }

    @Test
    @DisplayName("Base search for playlist test")
    void searchForPlaylist() throws IOException, SpotifyWebApiException {
        final int testCount = 10;
        final String testSearchTerm = "search";

        final List<WrappedPlaylist> expectedPlaylists = buildWrappedPlaylists(testCount);
        final Paging<PlaylistSimplified> mockPlaylistPage = buildSimplifiedPlaylistPage(testCount);
        when(mockSpotifyApiWrapper.searchForPlaylist(testSearchTerm)).thenReturn(mockPlaylistPage);

        assertEquals(expectedPlaylists, spotifyApiDataAccessor.searchForPlaylist(testSearchTerm));
    }

    @Test
    @DisplayName("Base get many albums test without pagination")
    void getManyAlbums() throws IOException, SpotifyWebApiException {
        final String testArtist = "test artist";
        final int testCount = 15;

        final List<WrappedAlbum> wrappedAlbums = buildWrappedAlbumList(testCount);
        final Map<String, WrappedAlbum> expectedAlbums = wrappedAlbums.stream()
                .collect(Collectors.toMap(WrappedAlbum::getSpotifyId, a -> a));

        final Paging<AlbumSimplified> albumPage = buildSimplifiedAlbumPage(testCount, 0);
        when(mockSpotifyApiWrapper.getSpotifyArtistsAlbums(testArtist, 0)).thenReturn(albumPage);

        final String[] albumIds = buildIdsArray(0, testCount);
        final Album[] spotifyAlbums = buildAlbums(testCount, 0);
        when(mockSpotifyApiWrapper.getSpotifyAlbums(albumIds)).thenReturn(spotifyAlbums);

        final Map<String, WrappedAlbum> returnedAlbums = spotifyApiDataAccessor.getManyAlbums(testArtist);

        verify(mockSpotifyApiWrapper, times(1)).getSpotifyArtistsAlbums(any(String.class), anyInt());
        verify(mockSpotifyApiWrapper, times(1)).getSpotifyAlbums(any(String[].class));

        assertEquals(expectedAlbums, returnedAlbums);
    }

    @Test
    @DisplayName("Get many albums test with pagination")
    void getManyAlbumsWithPagination() throws IOException, SpotifyWebApiException {
        final String testArtist = "test artist with pagination";
        final int testCount = 65;

        final List<WrappedAlbum> wrappedAlbums = buildWrappedAlbumList(testCount);
        final Map<String, WrappedAlbum> expectedAlbums = wrappedAlbums.stream()
                .collect(Collectors.toMap(WrappedAlbum::getSpotifyId, a -> a));

        final Paging<AlbumSimplified> albumPage0 = buildSimplifiedAlbumPage(testCount, 0);
        final Paging<AlbumSimplified> albumPage1 =
                buildSimplifiedAlbumPage(testCount, SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE);
        when(mockSpotifyApiWrapper.getSpotifyArtistsAlbums(testArtist, 0)).thenReturn(albumPage0);
        when(mockSpotifyApiWrapper.getSpotifyArtistsAlbums(testArtist, SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE))
                .thenReturn(albumPage1);

        final String[] albumIds0 = buildIdsArray(0, SpotifyApiConstants.ALBUM_PAGE_SIZE);
        final String[] albumIds1 = buildIdsArray(SpotifyApiConstants.ALBUM_PAGE_SIZE, SpotifyApiConstants.ALBUM_PAGE_SIZE);
        final String[] albumIds2 = buildIdsArray(SpotifyApiConstants.ALBUM_PAGE_SIZE * 2, SpotifyApiConstants.ALBUM_PAGE_SIZE);
        final String[] albumIds3 = buildIdsArray(SpotifyApiConstants.ALBUM_PAGE_SIZE * 3, testCount - (SpotifyApiConstants.ALBUM_PAGE_SIZE * 3));
        final Album[] spotifyAlbums0 = buildAlbums(testCount, 0);
        final Album[] spotifyAlbums1 = buildAlbums(testCount, SpotifyApiConstants.ALBUM_PAGE_SIZE);
        final Album[] spotifyAlbums2 = buildAlbums(testCount, SpotifyApiConstants.ALBUM_PAGE_SIZE * 2);
        final Album[] spotifyAlbums3 = buildAlbums(testCount, SpotifyApiConstants.ALBUM_PAGE_SIZE * 3);
        when(mockSpotifyApiWrapper.getSpotifyAlbums(albumIds0)).thenReturn(spotifyAlbums0);
        when(mockSpotifyApiWrapper.getSpotifyAlbums(albumIds1)).thenReturn(spotifyAlbums1);
        when(mockSpotifyApiWrapper.getSpotifyAlbums(albumIds2)).thenReturn(spotifyAlbums2);
        when(mockSpotifyApiWrapper.getSpotifyAlbums(albumIds3)).thenReturn(spotifyAlbums3);

        final Map<String, WrappedAlbum> returnedAlbums = spotifyApiDataAccessor.getManyAlbums(testArtist);

        verify(mockSpotifyApiWrapper, times(2)).getSpotifyArtistsAlbums(any(String.class), anyInt());
        verify(mockSpotifyApiWrapper, times(4)).getSpotifyAlbums(any(String[].class));

        assertEquals(expectedAlbums, returnedAlbums);
    }

    @Test
    @DisplayName("Base get album tracks test one album without pagination")
    void getAlbumTracks() throws IOException, SpotifyWebApiException {
        final String testAlbum = "test album";
        final int testCount = 30;

        final List<String> expectedTrackIds = Arrays.asList(buildIdsArray(0, testCount));

        final Paging<TrackSimplified> trackIds = buildSimplifiedTrackPage(testCount, 0, "");
        when(mockSpotifyApiWrapper.getSpotifyAlbumTracks(testAlbum, 0)).thenReturn(trackIds);

        final List<String> returnedTrackIds = spotifyApiDataAccessor.getAlbumTracks(Lists.newArrayList(testAlbum));
        verify(mockSpotifyApiWrapper, times(1)).getSpotifyAlbumTracks(any(String.class), anyInt());

        assertEquals(expectedTrackIds, returnedTrackIds);
    }

    @Test
    @DisplayName("Get album tracks with multiple albums and pagination")
    void getAlbumTracksWithPagination() throws IOException, SpotifyWebApiException {
        final String testAlbum1 = "testAlbum1-";
        final String testAlbum2 = "testAlbum2-";
        final int testCount1 = 10;
        final int testCount2 = 135;

        final List<String> totalExpectedTrackIds = ListUtils.union(
                Arrays.asList(buildIdsArray(testCount1, testAlbum1)),
                Arrays.asList(buildIdsArray(testCount2, testAlbum2)));

        final Paging<TrackSimplified> album1TrackIds = buildSimplifiedTrackPage(testCount1, 0, testAlbum1);
        when(mockSpotifyApiWrapper.getSpotifyAlbumTracks(testAlbum1, 0)).thenReturn(album1TrackIds);

        final Paging<TrackSimplified> album2TrackIds0 = buildSimplifiedTrackPage(testCount2, 0, testAlbum2);
        final Paging<TrackSimplified> album2TrackIds1 = buildSimplifiedTrackPage(testCount2, SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE, testAlbum2);
        final Paging<TrackSimplified> album2TrackIds2 = buildSimplifiedTrackPage(testCount2, SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE * 2, testAlbum2);
        when(mockSpotifyApiWrapper.getSpotifyAlbumTracks(testAlbum2, 0)).thenReturn(album2TrackIds0);
        when(mockSpotifyApiWrapper.getSpotifyAlbumTracks(testAlbum2, SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE)).thenReturn(album2TrackIds1);
        when(mockSpotifyApiWrapper.getSpotifyAlbumTracks(testAlbum2, SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE * 2)).thenReturn(album2TrackIds2);

        final List<String> returnedTrackIds = spotifyApiDataAccessor.getAlbumTracks(Lists.newArrayList(testAlbum1, testAlbum2));
        verify(mockSpotifyApiWrapper, times(4)).getSpotifyAlbumTracks(any(String.class), anyInt());

        assertEquals(totalExpectedTrackIds, returnedTrackIds);
    }

    private String[] buildIdsArray(int start, final int count) {
        final String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = Integer.toString(start++);
        }
        return ids;
    }

    private String[] buildIdsArray(final int count, final String prefix) {
        final String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = prefix + i;
        }
        return ids;
    }

    private List<WrappedArtist> buildWrappedArtistList(final int itemCount) {
        final List<WrappedArtist> wrappedArtists = new ArrayList<>();
        final List<String> testGenres = new ArrayList<>();
        testGenres.add("genre 1");
        testGenres.add("genre 2");

        for (int i = 0; i < itemCount; i++) {
            final String itemString = Integer.toString(i);
            final WrappedArtist artist = new WrappedArtist(itemString, testGenres, itemString, i, null);
            wrappedArtists.add(artist);
        }

        return wrappedArtists;
    }

    private Paging<Artist> buildArtistPage(final int itemCount) {
        final Paging.Builder<Artist> artistPageBuilder = new Paging.Builder<>();

        final Artist[] artistPageItems = new Artist[itemCount];
        for (int i = 0; i < itemCount; i++) {
            final Artist.Builder artistBuilder = new Artist.Builder();
            final String id = Integer.toString(i);
            artistBuilder.setName(id);
            artistBuilder.setId(id);
            artistBuilder.setPopularity(i);
            artistBuilder.setGenres("genre 1", "genre 2");
            final Artist item = artistBuilder.build();
            artistPageItems[i] = item;
        }

        artistPageBuilder.setItems(artistPageItems);
        artistPageBuilder.setTotal(itemCount);
        return artistPageBuilder.build();
    }

    private List<WrappedAlbum> buildWrappedAlbumList(final int itemCount) {
        final List<WrappedAlbum> wrappedAlbums = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            final String itemString = Integer.toString(i);

            final List<String> artistIds = new ArrayList<>();
            artistIds.add("artist " + itemString);
            final List<String> genres = new ArrayList<>();
            genres.add("genre " + itemString);
            final List<String> imageUrls = new ArrayList<>();
            imageUrls.add("image url " + itemString);
            final List<String> trackIds = new ArrayList<>();
            trackIds.add("track " + itemString);

            final WrappedAlbum album = new WrappedAlbum(itemString,
                    artistIds,
                    "ALBUM",
                    genres,
                    "name " + itemString,
                    i,
                    imageUrls,
                    trackIds,
                    "releasedate " + itemString,
                    "DAY");
            wrappedAlbums.add(album);
        }

        return wrappedAlbums;
    }

    private Paging<AlbumSimplified> buildSimplifiedAlbumPage(final int itemCount, final int offset) {
        final Paging.Builder<AlbumSimplified> albumPageBuilder = new Paging.Builder<>();

        final int remainingItemCount = itemCount - offset;
        int pageItemCount = remainingItemCount > SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE ?
                SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE : remainingItemCount;

        final AlbumSimplified[] albumPageItems = new AlbumSimplified[pageItemCount];
        for (int i = offset; i < (offset + pageItemCount); i++) {
            final AlbumSimplified.Builder albumBuilder = new AlbumSimplified.Builder();
            final String itemString = Integer.toString(i);

            albumBuilder.setId(itemString);
            albumPageItems[i - offset] = albumBuilder.build();
        }

        albumPageBuilder.setTotal(itemCount);
        albumPageBuilder.setItems(albumPageItems);
        return albumPageBuilder.build();
    }

    private Album[] buildAlbums(final int itemCount, final int offset) {
        final int remainingItemCount = itemCount - offset;
        int pageItemCount = remainingItemCount > SpotifyApiConstants.ALBUM_PAGE_SIZE ?
                SpotifyApiConstants.ALBUM_PAGE_SIZE : remainingItemCount;

        final Album[] albums = new Album[pageItemCount];

        for (int i = offset; i < (offset + pageItemCount); i++) {
            final Album.Builder albumBuilder = new Album.Builder();
            final String itemString = Integer.toString(i);

            albumBuilder.setId(itemString);
            albumBuilder.setAlbumType(AlbumType.ALBUM);
            final ArtistSimplified artist = new ArtistSimplified.Builder().setId("artist " + itemString).build();
            albumBuilder.setArtists(artist);
            albumBuilder.setGenres("genre " + itemString);
            albumBuilder.setName("name " + itemString);
            albumBuilder.setPopularity(i);
            final Image image = new Image.Builder().setUrl("image url " + itemString).build();
            albumBuilder.setImages(image);
            final Paging.Builder<TrackSimplified> tracksBuilder = new Paging.Builder<>();
            final TrackSimplified track = new TrackSimplified.Builder().setId("track " + itemString).build();
            tracksBuilder.setItems(new TrackSimplified[] { track });
            albumBuilder.setTracks(tracksBuilder.build());
            albumBuilder.setReleaseDate("releasedate " + itemString);
            albumBuilder.setReleaseDatePrecision(ReleaseDatePrecision.DAY);

            albums[i - offset] = albumBuilder.build();
        }

        return albums;
    }

    private List<WrappedPlaylist> buildWrappedPlaylists(final int itemCount) {
        final List<WrappedPlaylist> playlists = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            final String itemString = Integer.toString(i);

            final List<String> imageUrls = new ArrayList<>();
            imageUrls.add("image url " + itemString);
            final WrappedPlaylist playlist = new WrappedPlaylist(itemString,
                    "user " + itemString,
                    "name " + itemString,
                    imageUrls
            );
            playlists.add(playlist);
        }

        return playlists;
    }

    private Paging<PlaylistSimplified> buildSimplifiedPlaylistPage(final int itemCount) {
        final Paging.Builder<PlaylistSimplified> playlistPageBuilder = new Paging.Builder<>();

        final PlaylistSimplified[] playlistPageItems = new PlaylistSimplified[itemCount];
        for (int i = 0; i < itemCount; i++) {
            final String itemString = Integer.toString(i);

            final PlaylistSimplified.Builder playlistBuilder = new PlaylistSimplified.Builder();
            playlistBuilder.setId(itemString);
            playlistBuilder.setName("name " + itemString);
            final User owner = new User.Builder().setId("user " + itemString).build();
            playlistBuilder.setOwner(owner);
            final Image image = new Image.Builder().setUrl("image url " + itemString).build();
            playlistBuilder.setImages(image);

            playlistPageItems[i] = playlistBuilder.build();
        }

        playlistPageBuilder.setItems(playlistPageItems);
        playlistPageBuilder.setTotal(itemCount);
        return playlistPageBuilder.build();
    }

    private Paging<TrackSimplified> buildSimplifiedTrackPage(final int itemCount, final int offset, final String prefix) {
        final Paging.Builder<TrackSimplified> trackPageBuilder = new Paging.Builder<>();

        final int remainingItemCount = itemCount - offset;
        int pageItemCount = remainingItemCount > SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE ?
                SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE : remainingItemCount;

        final TrackSimplified[] trackPageItems = new TrackSimplified[pageItemCount];
        for (int i = offset; i < (offset + pageItemCount); i++) {
            final String itemString = prefix + i;

            final TrackSimplified.Builder trackBuilder = new TrackSimplified.Builder();
            trackBuilder.setId(itemString);

            trackPageItems[i - offset] = trackBuilder.build();
        }
        trackPageBuilder.setItems(trackPageItems);
        trackPageBuilder.setTotal(itemCount);

        return trackPageBuilder.build();
    }
}