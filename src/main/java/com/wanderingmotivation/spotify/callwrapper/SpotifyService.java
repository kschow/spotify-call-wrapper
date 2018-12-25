package com.wanderingmotivation.spotify.callwrapper;

import com.wanderingmotivation.spotify.callwrapper.model.WrappedAlbum;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedPlaylist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedTrack;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class SpotifyService {
    private static final Logger LOGGER = Logger.getLogger(SpotifyService.class);

    private final SpotifyCallWrapper spotifyCallWrapper;

    SpotifyService(SpotifyCallWrapper spotifyCallWrapper) {
        this.spotifyCallWrapper = spotifyCallWrapper;
    }

    /**
     * Searches for an artist
     * @param search search parameter matching spotify-web-api-java's SearchArtistsRequest
     * @return list of artists returned
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    @GetMapping("/search/artist")
    public List<WrappedArtist> searchForArtist(@RequestParam final String search)
            throws IOException, SpotifyWebApiException {
        return spotifyCallWrapper.searchForArtist(search);
    }

    /**
     * Searches for an album
     * @param search search parameter matching spotify-web-api-java's SearchAlbumsRequest
     * @return list of albums returned
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    @GetMapping("/search/album")
    public List<WrappedAlbum> searchForAlbum(@RequestParam final String search)
            throws IOException, SpotifyWebApiException {
        return spotifyCallWrapper.searchForAlbum(search);
    }

    /**
     * Searches for a playlist
     * @param search search parameter matching spotify-web-api-java's SearchPlaylistsRequest
     * @return list of playlists returned
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    @GetMapping("/search/playlist")
    public List<WrappedPlaylist> searchForPlaylist(@RequestParam final String search)
            throws IOException, SpotifyWebApiException {
        return spotifyCallWrapper.searchForPlaylist(search);
    }

    /**
     * Get full track information for an artist
     * @param artistId Spotify URI for an artist
     * @return Map of artist, track, and album information
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    @GetMapping("/getArtistTracks/{artistId}")
    public Map<String, WrappedTrack> getArtistTracks(@PathVariable final String artistId)
            throws SpotifyWebApiException, IOException {
        return spotifyCallWrapper.getArtistTracks(artistId);
    }

    /**
     * Gets all tracks for a playlist
     * Deprecated until associated change made in visualizer
     * @param userId owner's user id
     * @param playlistId Spotify playlist id
     * @return map of track id to track information
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    @GetMapping("/getPlaylistTracks/{userId}/{playlistId}")
    @Deprecated
    public Map<String, WrappedTrack> getPlaylistTracks(@PathVariable final String userId,
                                                       @PathVariable final String playlistId)
            throws IOException, SpotifyWebApiException {
        return spotifyCallWrapper.getPlaylistTracks(playlistId);
    }

    /**
     * Gets all tracks for a playlist
     * @param playlistId Spotify playlist id
     * @return map of track id to track information
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    @GetMapping("/getPlaylistTracks/{playlistId}")
    @Deprecated
    public Map<String, WrappedTrack> getPlaylistTracks(@PathVariable final String playlistId)
            throws IOException, SpotifyWebApiException {
        return spotifyCallWrapper.getPlaylistTracks(playlistId);
    }
}
