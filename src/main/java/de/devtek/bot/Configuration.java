package de.devtek.bot;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

/**
 * @author Biosphere
 * @date 05.01.2018
 */
@Getter
public class Configuration {

    private String token;
    private boolean twitter;
    private HashMap<String, String> twitterCredentials;
    @Setter
    private List<String> postetThreads;

}
