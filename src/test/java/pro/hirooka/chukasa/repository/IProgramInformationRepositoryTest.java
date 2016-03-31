package pro.hirooka.chukasa.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pro.hirooka.chukasa.Application;
import pro.hirooka.chukasa.configuration.ChukasaConfiguration;
import pro.hirooka.chukasa.domain.ProgramInformation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest
public class IProgramInformationRepositoryTest {

    @Autowired
    ChukasaConfiguration chukasaConfiguration;

    @Autowired
    IProgramInformationRepository programInformationRepository;

    @Ignore
    @Test
    public void findOneNow(){
        long now = new Date().getTime();
        log.info("now = {}", now);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddhhmm");
        String nowString = simpleDateFormat.format(new Date());
        log.info("nowString = {}", nowString);
        ProgramInformation programInformation = programInformationRepository.findOneNowByChAndNowLike(20, Long.parseLong(nowString));
        log.info(programInformation.toString());
    }

    @Ignore
    @Test
    public void findAllNow(){
        long now = new Date().getTime();
        log.info("now = {}", now);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddhhmm");
        String nowString = simpleDateFormat.format(new Date());
        log.info("nowString = {}", nowString);
        List<ProgramInformation> programInformationList = programInformationRepository.findAllNowByChAndNowLike(Long.parseLong(nowString));
        log.info(programInformationList.toString());
    }

    @Ignore
    @Test
    public void test(){
        Integer[] physicalChannelArray = chukasaConfiguration.getPhysicalChannel();
        assertThat(physicalChannelArray.length, is(42));
        List<ProgramInformation> programInformationList = programInformationRepository.findAllByBeginDateLike("");
        log.info("size = {}", programInformationList.size());
        assertThat(true, is(true));
    }
}
