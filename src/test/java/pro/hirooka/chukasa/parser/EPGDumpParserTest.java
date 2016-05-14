package pro.hirooka.chukasa.parser;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pro.hirooka.chukasa.Application;
import pro.hirooka.chukasa.epgdump.EpgdumpParser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest
public class EPGDumpParserTest {

    @Autowired
    EpgdumpParser epgDumpParser;

    @Ignore
    @Test
    public void test(){
        //epgDumpParser.parse("/tmp/epgdump.json");
    }
}
