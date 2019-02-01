package com.wanderingmotivation.spotify.callwrapper.model;

import com.wrapper.spotify.model_objects.specification.Image;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class WrappedPlaylist {
    private String spotifyId;
    private String userId;
    private String name;
    private List<String> imageUrls;

    public WrappedPlaylist(final PlaylistSimplified playlist) {
        this.spotifyId = playlist.getId();
        this.name = playlist.getName();
        this.userId = playlist.getOwner().getId();
        this.imageUrls = Arrays.stream(playlist.getImages())
                .map(Image::getUrl)
                .collect(Collectors.toList());
    }
}
