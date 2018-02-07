package com.wanderingmotivation.spotify.callwrapper.model;

import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.AudioFeatures;
import com.wrapper.spotify.model_objects.specification.Track;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A combination of track information and its related audio features
 */
@Data
public class WrappedTrack {
    private String name;
    private String spotifyId;
    private List<String> artistIds;
    private String albumId;
    private List<String> availableMarkets;
    private Integer popularity;
    private Integer trackNumber;
    private Float danceability;
    private Float energy;
    private Integer key;
    private Float loudness;
    private Integer mode;
    private Float speechiness;
    private Float acousticness;
    private Float instrumentalness;
    private Float liveness;
    private Float valence;
    private Float tempo;
    private Integer durationMs;
    private Integer timeSignature;

    public WrappedTrack(final Track track) {
        super();
        setTrackProperties(track);
    }

    public WrappedTrack(final AudioFeatures audioFeatures) {
        super();
        setAudioFeatures(audioFeatures);
    }

    public void setTrackProperties(final Track track) {
        name = track.getName();
        spotifyId = track.getId();
        artistIds = Arrays.stream(track.getArtists())
                .map(ArtistSimplified::getId)
                .collect(Collectors.toList());
        albumId = track.getAlbum().getId();
        availableMarkets = Arrays.stream(track.getAvailableMarkets())
                .map(CountryCode::toString)
                .collect(Collectors.toList());
        popularity = track.getPopularity();
        trackNumber = track.getTrackNumber();
    }

    public void setAudioFeatures(final AudioFeatures audioFeatures) {
        danceability = audioFeatures.getDanceability();
        energy = audioFeatures.getEnergy();
        key = audioFeatures.getKey();
        loudness = audioFeatures.getLoudness();
        mode = audioFeatures.getMode().getType();
        speechiness = audioFeatures.getSpeechiness();
        acousticness = audioFeatures.getAcousticness();
        instrumentalness = audioFeatures.getInstrumentalness();
        liveness = audioFeatures.getLiveness();
        valence = audioFeatures.getValence();
        tempo = audioFeatures.getTempo();
        durationMs = audioFeatures.getDurationMs();
        timeSignature = audioFeatures.getTimeSignature();
    }
}
