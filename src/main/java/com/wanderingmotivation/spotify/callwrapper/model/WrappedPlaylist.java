package com.wanderingmotivation.spotify.callwrapper.model;

import com.wrapper.spotify.model_objects.specification.Image;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class WrappedPlaylist {
    private String spotifyId;
    private String userId;
    private String name;
    private List<Image> images;

    public WrappedPlaylist(final PlaylistSimplified playlist) {
        this.spotifyId = playlist.getId();
        this.userId = playlist.getOwner().getId();
        this.name = playlist.getName();
        this.images = Arrays.stream(playlist.getImages()).collect(Collectors.toList());
    }
}
