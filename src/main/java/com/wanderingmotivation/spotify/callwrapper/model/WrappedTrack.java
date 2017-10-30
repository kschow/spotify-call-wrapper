package com.wanderingmotivation.spotify.callwrapper.model;

import com.wrapper.spotify.models.AudioFeature;
import com.wrapper.spotify.models.SimpleArtist;
import com.wrapper.spotify.models.Track;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A combination of track information and its related audio features
 */
@Data
public class WrappedTrack {
    private String name;
    private String trackId;
    private List<String> artistIds;
    private String albumId;
    private List<String> availableMarkets;
    private Integer popularity;
    private Integer trackNumber;
    private Double danceability;
    private Double energy;
    private Integer key;
    private Double loudness;
    private Integer mode;
    private Double speechiness;
    private Double acousticness;
    private Double instrumentalness;
    private Double liveness;
    private Double valence;
    private Double tempo;
    private Integer durationMs;
    private Integer timeSignature;

    public WrappedTrack(final Track track) {
        super();
        setTrackProperties(track);
    }

    public WrappedTrack(final AudioFeature audioFeature) {
        super();
        setAudioFeatures(audioFeature);
    }

    public void setTrackProperties(final Track track) {
        name = track.getName();
        trackId = track.getId();
        artistIds = track.getArtists().stream().map(SimpleArtist::getId).collect(Collectors.toList());
        albumId = track.getAlbum().getId();
        availableMarkets = track.getAvailableMarkets();
        popularity = track.getPopularity();
        trackNumber = track.getTrackNumber();
    }

    public void setAudioFeatures(final AudioFeature audioFeature) {
        danceability = audioFeature.getDanceability();
        energy = audioFeature.getEnergy();
        key = audioFeature.getKey();
        loudness = audioFeature.getLoudness();
        mode = audioFeature.getMode();
        speechiness = audioFeature.getSpeechiness();
        acousticness = audioFeature.getAcousticness();
        instrumentalness = audioFeature.getInstrumentalness();
        liveness = audioFeature.getLiveness();
        valence = audioFeature.getValence();
        tempo = audioFeature.getTempo();
        durationMs = audioFeature.getDurationMs();
        timeSignature = audioFeature.getTimeSignature();
    }
}
