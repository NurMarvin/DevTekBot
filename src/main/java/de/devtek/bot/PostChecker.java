package de.devtek.bot;

import com.google.gson.*;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.XML;
import org.jsoup.Jsoup;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author Biosphere
 * @date 10.02.18
 */
public class PostChecker {

    private JDA jda;
    private Twitter twitter;
    private Configuration configuration;
    private List<String> postetThreads;

    public PostChecker(JDA jda, Configuration configuration) {
        this.jda = jda;
        this.configuration = configuration;
        this.postetThreads = configuration.getPostetThreads();

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(configuration.getTwitterCredentials().get("OAuthConsumerKey"))
                .setOAuthConsumerSecret(configuration.getTwitterCredentials().get("OAuthConsumerSecret"))
                .setOAuthAccessToken(configuration.getTwitterCredentials().get("OAuthAccessToken"))
                .setOAuthAccessTokenSecret(configuration.getTwitterCredentials().get("OAuthAccessTokenSecret"));
        TwitterFactory factory = new TwitterFactory(cb.build());
        twitter = factory.getInstance();
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                JsonObject jsonObject = null;
                try {
                    URLConnection connection = new URL("https://dev-tek.de/forum/index.php?board-feed/").openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                    connection.connect();
                    jsonObject = new JsonParser().parse(XML.toJSONObject(IOUtils.toString(new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")))).toString()).getAsJsonObject();
                    connection.close();
                } catch (IOException e) {
                    System.out.println("Could not parse latest posts " + e.getMessage());
                }
                if (jsonObject != null) {
                    for (JsonElement jsonElement : jsonObject.get("rss").getAsJsonObject().get("channel").getAsJsonObject().get("item").getAsJsonArray()) {
                        if (!postetThreads.contains(jsonElement.getAsJsonObject().get("guid").getAsString())) {
                            String description = Jsoup.parse(jsonElement.getAsJsonObject().get("content:encoded").getAsString().replaceAll("(?i)<br[^>]*>", "br2nl").replaceAll("\n", "br2nl")).text().trim();
                            description = description.replaceAll("br2nl", "");
                            if (description.length() >= 241) {
                                description = description.substring(0, 240) + " ...";
                            }

                            postetThreads.add(jsonElement.getAsJsonObject().get("guid").getAsString());
                            configuration.setPostetThreads(postetThreads);
                            try {
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                FileUtils.writeStringToFile(new File("config.json"), gson.toJson(configuration));
                            } catch (IOException e) {
                                System.out.println("Could not save config "  + e.getMessage());
                            }
                            EmbedBuilder embedBuilder = new EmbedBuilder();
                            embedBuilder.setColor(Color.CYAN);
                            embedBuilder.setFooter("@" + jsonElement.getAsJsonObject().get("dc:creator").getAsString(), "https://dev-tek.de/images/apple-touch-icon.png");
                            embedBuilder.setTitle(jsonElement.getAsJsonObject().get("title").getAsString(), jsonElement.getAsJsonObject().get("link").getAsString());
                            embedBuilder.addField(description, "", false);
                            jda.getGuilds().forEach(guild -> {
                                if(!guild.getTextChannelsByName("dev-tek", true).isEmpty()){
                                    if(PermissionUtil.checkPermission( guild.getTextChannelsByName("dev-tek", true).get(0), guild.getSelfMember(), Permission.MESSAGE_WRITE)){
                                        guild.getTextChannelsByName("dev-tek", true).get(0).sendMessage(embedBuilder.build()).queue();
                                    }
                                }
                            });

                            if(configuration.isTwitter()){
                                try {
                                    twitter.updateStatus(jsonElement.getAsJsonObject().get("guid").getAsString());
                                } catch (TwitterException ex) {
                                    System.out.println("Could not post tweet " + ex.getMessage());
                                }
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(1000 * 30);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
