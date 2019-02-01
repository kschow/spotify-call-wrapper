package com.wanderingmotivation.spotify.callwrapper.api.spotify;

import com.wanderingmotivation.spotify.callwrapper.model.WrappedAlbum;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wrapper.spotify.enums.AlbumType;
import com.wrapper.spotify.enums.ReleaseDatePrecision;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.AlbumSimplified;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Image;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
        final Paging<AlbumSimplified> mockSimpleAlbumPage = buildSimplifiedAlbumPage(testCount);
        final String[] mockAlbumIds = buildIdsArray(0, testCount);
        final Album[] mockAlbums = buildAlbums(testCount);
        when(mockSpotifyApiWrapper.searchForAlbum(testSearchTerm)).thenReturn(mockSimpleAlbumPage);
        when(mockSpotifyApiWrapper.getSpotifyAlbums(mockAlbumIds)).thenReturn(mockAlbums);

        assertEquals(expectedAlbums, spotifyApiDataAccessor.searchForAlbum(testSearchTerm));
    }

    @Test
    void searchForPlaylist() {
    }

    @Test
    void getArtistTracks() {
    }

    @Test
    void getPlaylistTracks() {
    }

    private String[] buildIdsArray(int start, int count) {
        final String[] ids = new String[count];
        int counter = 0;

        while (counter < count) {
            ids[counter++] = Integer.toString(start++);
        }
        return ids;
    }

    private List<WrappedArtist> buildWrappedArtistList(int itemCount) {
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

    private Paging<Artist> buildArtistPage(int itemCount) {
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

    private List<WrappedAlbum> buildWrappedAlbumList(int itemCount) {
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

    private Paging<AlbumSimplified> buildSimplifiedAlbumPage(int itemCount) {
        final Paging.Builder<AlbumSimplified> albumPageBuilder = new Paging.Builder<>();

        final AlbumSimplified[] albumPageItems = new AlbumSimplified[itemCount];
        for (int i = 0; i < itemCount; i++) {
            final AlbumSimplified.Builder albumBuilder = new AlbumSimplified.Builder();
            final String itemString = Integer.toString(i);

            albumBuilder.setId(itemString);
            albumPageItems[i] = albumBuilder.build();
        }
        albumPageBuilder.setTotal(itemCount);
        albumPageBuilder.setItems(albumPageItems);
        return albumPageBuilder.build();
    }

    private Album[] buildAlbums(int itemCount) {
        final Album[] albums = new Album[itemCount];

        for (int i = 0; i < itemCount; i++) {
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

            albums[i] = albumBuilder.build();
        }
        return albums;
    }
}