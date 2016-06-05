package pro.hirooka.chukasa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import pro.hirooka.chukasa.configuration.ChukasaConfiguration;
import pro.hirooka.chukasa.configuration.SystemConfiguration;
import pro.hirooka.chukasa.domain.chukasa.VideoFileModel;
import pro.hirooka.chukasa.domain.recorder.Program;
import pro.hirooka.chukasa.service.recorder.IProgramTableService;
import pro.hirooka.chukasa.service.epgdump.ILastEpgdumpExecutedService;
import pro.hirooka.chukasa.service.system.ISystemService;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Controller
public class IndexController {

    @Autowired
    SystemConfiguration systemConfiguration;
    @Autowired
    ChukasaConfiguration chukasaConfiguration;
    @Autowired
    ISystemService systemService;
    @Autowired
    IProgramTableService programTableService;
    @Autowired
    ILastEpgdumpExecutedService lastEpgdumpExecutedService;
    @Autowired
    private HttpServletRequest httpServletRequest;

    @RequestMapping("/")
    String index(Model model){

        boolean isSupported = false;
        String userAgent = httpServletRequest.getHeader("user-agent");
        if((userAgent.contains("Mac OS X 10_11") && (userAgent.contains("Version") && userAgent.split("Version/")[1].split(" ")[0].contains("9")))
                || (userAgent.contains("iPhone OS 9") && (userAgent.contains("Version") && userAgent.split("Version/")[1].split(" ")[0].contains("9")))
                || (userAgent.contains("iPad; CPU OS 9") && (userAgent.contains("Version") && userAgent.split("Version/")[1].split(" ")[0].contains("9")))
                || (userAgent.contains("Windows") && userAgent.contains("Edge/"))
                || (userAgent.contains("Chrome"))){
            isSupported = true;
        }
        log.info("{} : {}", isSupported, userAgent);

        boolean isFFmpeg = systemService.isFFmpeg();
        boolean isPTx = systemService.isPTx();
        boolean isRecpt1 = systemService.isRecpt1();
        boolean isEpgdump = systemService.isEpgdump();
        boolean isMongoDB = systemService.isMongoDB();
        boolean isWebCamera = systemService.isWebCamera();

        // PTx
        List<Program> programList = new ArrayList<>();
        boolean isLastEpgdumpExecuted = false;
        Map<String, Integer> epgdumpChannelMap = new HashMap<>();
        Resource resource = new ClassPathResource("epgdump_channel_map.json");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            epgdumpChannelMap = objectMapper.readValue(resource.getInputStream(), HashMap.class);
            log.info(epgdumpChannelMap.toString());
        } catch (IOException e) {
            log.error("invalid epgdump_channel_map.json: {} {}", e.getMessage(), e);
        }
        if(isMongoDB && isEpgdump){
            programList = programTableService.readByNow(new Date().getTime());
            if(programList != null && lastEpgdumpExecutedService.read(1) != null && programTableService.getNumberOfPhysicalChannels() >= epgdumpChannelMap.size()){
                isLastEpgdumpExecuted = true;
            }
        }

        // PTx (switch Program/Channel)
        boolean isPTxByProgram = false;
        if(isFFmpeg && isPTx && isRecpt1 && isLastEpgdumpExecuted){
            isPTxByProgram = true;
        }
        boolean isPTxByChannel = false;
        if(isFFmpeg && isPTx && isRecpt1 && !isLastEpgdumpExecuted){
            programList = new ArrayList<>();
            isPTxByChannel = true;
            //if(epgDumpProgramInformationList.size() == 0) {
                for (Map.Entry<String, Integer> entry : epgdumpChannelMap.entrySet()) {
                    Program program = new Program();
                    program.setCh(entry.getValue());
                    programList.add(program);
                }
            //}
        }

        // FILE
        List<VideoFileModel> videoFileModelList = new ArrayList<>();
        File fileDirectory = new File(systemConfiguration.getFilePath());
        File[] fileArray = fileDirectory.listFiles();
        if(fileArray != null) {
            String[] videoFileExtensionArray = chukasaConfiguration.getVideoFileExtension();
            List<String> videoFileExtensionList = Arrays.asList(videoFileExtensionArray);
            for (File file : fileArray) {
                for(String videoFileExtension : videoFileExtensionList){
                    if(file.getName().endsWith("." + videoFileExtension)){
                        VideoFileModel videoFileModel = new VideoFileModel();
                        videoFileModel.setName(file.getName());
                        videoFileModelList.add(videoFileModel);
                    }
                }
            }
        }else{
            log.warn("'{}' does not exist.", fileDirectory);
        }

        // Okkake
        List<VideoFileModel> okkakeVideoFileModelList = new ArrayList<>();
        File okkakeVideoFileDirectory = new File(systemConfiguration.getFilePath());
        File[] okkakeVideoFileArray = okkakeVideoFileDirectory.listFiles();
        if(okkakeVideoFileArray != null) {
            for (File file : fileArray) {
                if(file.getName().endsWith(".ts")){
                    VideoFileModel okkakeVideoFileModel = new VideoFileModel();
                    okkakeVideoFileModel.setName(file.getName());
                    okkakeVideoFileModelList.add(okkakeVideoFileModel);
                }
            }
        }else{
            log.warn("'{}' does not exist.", okkakeVideoFileDirectory);
        }

        model.addAttribute("isSupported", isSupported);
        model.addAttribute("isPTxByChannel", isPTxByChannel);
        model.addAttribute("isPTxByProgram", isPTxByProgram);
        model.addAttribute("isWebCamera", isWebCamera);
        model.addAttribute("videoFileModelList", videoFileModelList);
        model.addAttribute("okkakeVideoFileModelList", okkakeVideoFileModelList);
        model.addAttribute("programList", programList);

        return "index";
    }
}
