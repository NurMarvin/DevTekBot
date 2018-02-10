package de.devtek.bot;

import com.google.gson.Gson;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * @author Biosphere
 * @date 10.02.18
 */
@Getter
public class DevTekBot {

    private DevTekBot(){
        try {
            final Configuration configuration = new Gson().fromJson(new FileReader("config.json"), Configuration.class); ;
            try {
                new JDABuilder(AccountType.BOT)
                        .setToken(configuration.getToken())
                        .setEnableShutdownHook(true)
                        .setStatus(OnlineStatus.ONLINE)
                        .setAutoReconnect(true)
                        .setAudioEnabled(false)
                        .addEventListener(new ListenerAdapter() {
                            @Override
                            public void onReady(ReadyEvent event) {
                                super.onReady(event);
                                new PostChecker(event.getJDA(), configuration).start();
                            }
                        })
                        .buildBlocking();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    public static void main(String... args){
        new DevTekBot();
    }
}
