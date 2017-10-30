package com.wanderingmotivation.spotify.callwrapper.model;

import lombok.Data;
import java.util.Map;

/**
 * Describes information returned to clients
 * This is returned to clients as JSON
 */
@Data
public class FrontEndData {
    final Map<String, Map> data;
}
