package com.wanderingmotivation.spotify.callwrapper.api.spotify;

import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Paging;
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

    private List<WrappedArtist> buildWrappedArtistList(int itemCount) {
        List<WrappedArtist> wrappedArtists = new ArrayList<>();
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

        Artist[] artistPageItems = new Artist[itemCount];
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

    @Test
    void searchForAlbum() {
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
}