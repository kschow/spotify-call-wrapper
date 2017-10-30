package com.wanderingmotivation.spotify.callwrapper.model;

import com.wrapper.spotify.models.Album;
import com.wrapper.spotify.models.AlbumType;
import com.wrapper.spotify.models.Image;
import com.wrapper.spotify.models.SimpleArtist;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A simplified model for albums
 */
@Data
public class WrappedAlbum {
    private String albumId;
    private List<String> artistIds;
    private AlbumType albumType;
    private List<String> availableMarkets;
    private List<String> genres;
    private String name;
    private int popularity;
    private List<String> imageUrls;
    private String releaseDate;
    private String releaseDatePrecision;

    public WrappedAlbum(final Album album) {
        this.albumId = album.getId();
        this.artistIds = album.getArtists()
                .stream()
                .map(SimpleArtist::getId)
                .collect(Collectors.toList());
        this.albumType = album.getAlbumType();
        this.availableMarkets = album.getAvailableMarkets();
        this.genres = album.getGenres();
        this.name = album.getName();
        this.imageUrls = album.getImages()
                .stream()
                .map(Image::getUrl)
                .collect(Collectors.toList());
        this.popularity = album.getPopularity();
        this.releaseDate = album.getReleaseDate();
        this.releaseDatePrecision = album.getReleaseDatePrecision();
    }
}
