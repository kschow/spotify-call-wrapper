package com.wanderingmotivation.spotify.callwrapper.model;

import com.wrapper.spotify.models.Artist;
import com.wrapper.spotify.models.Image;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simplified model for an artist
 */
@Data
public class WrappedArtist {
    private String artistId;
    private List<String> genres;
    private String name;
    private int popularity;
    private List<String> imageUrls;

    public WrappedArtist(final Artist artist) {
        this.artistId = artist.getId();
        this.genres = artist.getGenres();
        this.name = artist.getName();
        this.popularity = artist.getPopularity();
        this.imageUrls = artist.getImages()
                .stream()
                .map(Image::getUrl)
                .collect(Collectors.toList());
    }
}
