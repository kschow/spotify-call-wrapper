package com.wanderingmotivation.spotify.callwrapper.model;

import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Image;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simplified model for an artist
 */
@Data
@AllArgsConstructor
public class WrappedArtist {
    private String spotifyId;
    private List<String> genres;
    private String name;
    private int popularity;
    private List<String> imageUrls;

    public WrappedArtist(final Artist artist) {
        this.spotifyId = artist.getId();
        this.genres = Arrays.asList(artist.getGenres());
        this.name = artist.getName();
        this.popularity = artist.getPopularity();
        this.imageUrls = artist.getImages() != null ?
                Arrays.stream(artist.getImages())
                        .map(Image::getUrl)
                        .collect(Collectors.toList()) :
                null;
    }
}
