package pro.hirooka.chukasa;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.context.annotation.Configuration;

@Configuration
public class M3U8MimeMapper implements EmbeddedServletContainerCustomizer{

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);
        mimeMappings.add("m3u8", "application/x-mpegURL");
        mimeMappings.add("ts", "video/MP2T");
        container.setMimeMappings(mimeMappings);
    }
}
