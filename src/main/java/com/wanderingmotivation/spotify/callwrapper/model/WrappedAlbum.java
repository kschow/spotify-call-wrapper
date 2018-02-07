package com.wanderingmotivation.spotify.callwrapper.model;

import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.enums.AlbumType;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Image;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simplified model for albums
 */
@Data
public class WrappedAlbum {
    private String spotifyId;
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
        this.spotifyId = album.getId();
        this.artistIds = Arrays.stream(album.getArtists())
                .map(ArtistSimplified::getId)
                .collect(Collectors.toList());
        this.albumType = album.getAlbumType();
        this.availableMarkets = Arrays.stream(album.getAvailableMarkets())
                .map(CountryCode::toString)
                .collect(Collectors.toList());
        this.genres = Arrays.asList(album.getGenres());
        this.name = album.getName();
        this.imageUrls = Arrays.stream(album.getImages())
                .map(Image::getUrl)
                .collect(Collectors.toList());
        this.popularity = album.getPopularity();
        this.releaseDate = album.getReleaseDate();
        this.releaseDatePrecision = album.getReleaseDatePrecision().toString();
    }
}
