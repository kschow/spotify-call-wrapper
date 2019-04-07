package com.wanderingmotivation.spotify.callwrapper.api.spotify;

import com.wanderingmotivation.spotify.callwrapper.model.WrappedAlbum;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedArtist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedPlaylist;
import com.wanderingmotivation.spotify.callwrapper.model.WrappedTrack;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.AlbumSimplified;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.AudioFeatures;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpotifyApiDataAccessor {
    private final SpotifyApiWrapper spotifyApiWrapper;

    SpotifyApiDataAccessor(SpotifyApiWrapper spotifyApiWrapper) {
        this.spotifyApiWrapper = spotifyApiWrapper;
    }

    /**
     * Searches for an artist
     * @param search search parameter matching spotify-web-api-java's SearchArtistsRequest
     * @return list of artists returned
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    public List<WrappedArtist> searchForArtist(final String search) throws IOException, SpotifyWebApiException {
        final Paging<Artist> spotifyArtists = spotifyApiWrapper.searchForArtist(search);

        return Arrays.stream(spotifyArtists.getItems())
                .map(WrappedArtist::new)
                .collect(Collectors.toList());
    }

    /**
     * Searches for an album
     * @param search search parameter matching spotify-web-api-java's SearchAlbumsRequest
     * @return list of albums returned
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    public List<WrappedAlbum> searchForAlbum(final String search) throws IOException, SpotifyWebApiException {
        final Paging<AlbumSimplified> simpleAlbums = spotifyApiWrapper.searchForAlbum(search);
        final String[] albumIds = Arrays.stream(simpleAlbums.getItems())
                .map(AlbumSimplified::getId)
                .collect(Collectors.toList())
                .toArray(new String[] {});

        final Album[] spotifyAlbums = spotifyApiWrapper.getSpotifyAlbums(albumIds);
        return Arrays.stream(spotifyAlbums)
                .map(WrappedAlbum::new)
                .collect(Collectors.toList());
    }

    /**
     * Searches for a playlist
     * @param search search parameter matching spotify-web-api-java's SearchPlaylistsRequest
     * @return list of playlists returned
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    public List<WrappedPlaylist> searchForPlaylist(final String search) throws IOException, SpotifyWebApiException {
        final Paging<PlaylistSimplified> simplePlaylists = spotifyApiWrapper.searchForPlaylist(search);

        return Arrays.stream(simplePlaylists.getItems())
                .map(WrappedPlaylist::new)
                .collect(Collectors.toList());
    }

    /**
     * Get full track information for an artist
     * @param artistId Spotify URI for an artist
     * @return Map of artist, track, and album information
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    public Map<String, WrappedTrack> getArtistTracks(final String artistId)
            throws SpotifyWebApiException, IOException {
        final long startTime = System.currentTimeMillis();
        log.debug("starting get artist track info");

        final Map<String, WrappedAlbum> albums = getManyAlbums(artistId);

        final List<String> albumIds = albums.values()
                .stream()
                .map(WrappedAlbum::getSpotifyId)
                .collect(Collectors.toList());

        final long albumTime = System.currentTimeMillis();
        log.debug("got album info, took: " + (albumTime - startTime) + "ms");

        final List<String> trackIds = getAlbumTracks(albumIds);
        final Map<String, WrappedTrack> tracks = getManyTracks(trackIds, artistId);

        final long trackTime = System.currentTimeMillis();
        log.debug("got track info, took: " + (trackTime - albumTime) + "ms");

        return tracks;
    }

    /**
     * Gets all tracks for a playlist
     * @param playlistId Spotify playlist id
     * @return map of track id to track information
     * @throws IOException
     * @throws SpotifyWebApiException Thrown when there is some Spotify error, e.g. TooManyRequestsException
     */
    public Map<String, WrappedTrack> getPlaylistTracks(final String playlistId)
            throws IOException, SpotifyWebApiException {
        final List<String> trackIds = new ArrayList<>();
        for (int offset = 0; ; offset += SpotifyApiConstants.PLAYLIST_TRACK_PAGE_SIZE) {
            final Paging<PlaylistTrack> page = spotifyApiWrapper.getSpotifyPlaylistTracks(playlistId, offset);
            final int totalTracks = page.getTotal();
            log.debug(String.format("getting ids for playlist tracks %s to %s out of %s",
                    offset, offset + SpotifyApiConstants.PLAYLIST_TRACK_PAGE_SIZE, totalTracks));
            trackIds.addAll(Arrays.stream(page.getItems())
                    .map(PlaylistTrack::getTrack)
                    .map(Track::getId)
                    .collect(Collectors.toList()));

            if (offset + SpotifyApiConstants.PLAYLIST_TRACK_PAGE_SIZE >= totalTracks) {
                break;
            }
        }
        return getManyTracks(trackIds, null);
    }

    Map<String, WrappedAlbum> getManyAlbums(final String artistId) throws IOException, SpotifyWebApiException {
        final List<String> albumIds = new ArrayList<>();

        for (int offset = 0; ; offset += SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE) {
            final Paging<AlbumSimplified> page = spotifyApiWrapper.getSpotifyArtistsAlbums(artistId, offset);
            final int totalAlbums = page.getTotal();
            log.debug(String.format("getting ids for artist albums %s to %s out of %s",
                    offset, offset + SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE, totalAlbums));
            albumIds.addAll(Arrays.stream(page.getItems())
                    .map(AlbumSimplified::getId)
                    .collect(Collectors.toList()));

            if (offset + SpotifyApiConstants.ARTIST_ALBUM_PAGE_SIZE >= totalAlbums) {
                break;
            }
        }

        final Map<String, WrappedAlbum> albums = new HashMap<>();
        final List<List<String>> partitions = ListUtils.partition(albumIds, SpotifyApiConstants.ALBUM_PAGE_SIZE);
        for (final List<String> chunk : partitions) {
            final String[] chunkArray = chunk.toArray(new String[] {});
            final Album[] spotifyAlbums = spotifyApiWrapper.getSpotifyAlbums(chunkArray);
            for (final Album a : spotifyAlbums) {
                albums.put(a.getId(), new WrappedAlbum(a));
            }
        }
        return albums;
    }

    List<String> getAlbumTracks(final List<String> albumIds) throws IOException, SpotifyWebApiException {
        final List<String> trackIds = new ArrayList<>();

        for (final String albumId : albumIds) {
            for (int offset = 0; ; offset += SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE) {
                final Paging<TrackSimplified> page = spotifyApiWrapper.getSpotifyAlbumTracks(albumId, offset);
                final int totalTracks = page.getTotal();
                log.debug(String.format("getting ids for album tracks %s to %s out of %s",
                        offset, offset + SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE, totalTracks));
                trackIds.addAll(Arrays.stream(page.getItems())
                        .map(TrackSimplified::getId)
                        .collect(Collectors.toList()));

                if (offset + SpotifyApiConstants.ALBUM_TRACK_PAGE_SIZE >= totalTracks) {
                    break;
                }
            }
        }
        return trackIds;
    }

    Map<String, WrappedTrack> getManyTracks(final List<String> trackIds, final String artistId)
            throws IOException, SpotifyWebApiException {
        final Map<String, WrappedTrack> tracks = new HashMap<>();
        final List<List<String>> partitions = ListUtils.partition(trackIds, SpotifyApiConstants.TRACK_PAGE_SIZE);

        int i = 0;
        final int totalTracks = trackIds.size();

        for (final List<String> chunk : partitions) {

            final String[] chunkArray = chunk.toArray(new String[] {});
            final Track[] spotifyTracks = spotifyApiWrapper.getSpotifyTracks(chunkArray);
            final AudioFeatures[] spotifyAudioFeatures = spotifyApiWrapper.getSpotifyAudioFeatures(chunkArray);

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
            log.debug(String.format("getting track information for %s to %s out of %s",
                    i, i += SpotifyApiConstants.TRACK_PAGE_SIZE, totalTracks));
        }

        return tracks.values()
                .stream()
                // remove tracks that might be on collaborative albums that don't include the artist
                // could do this earlier but the format of ArtistSimplified makes it a bit more annoying
                .filter(t -> artistId == null || t.getArtistIds().contains(artistId))
                .collect(Collectors.toMap(WrappedTrack::getSpotifyId, t -> t));
    }
}
