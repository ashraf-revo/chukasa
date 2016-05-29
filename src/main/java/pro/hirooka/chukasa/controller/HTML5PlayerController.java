package pro.hirooka.chukasa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import pro.hirooka.chukasa.configuration.ChukasaConfiguration;
import pro.hirooka.chukasa.configuration.HLSConfiguration;
import pro.hirooka.chukasa.configuration.SystemConfiguration;
import pro.hirooka.chukasa.domain.chukasa.ChukasaModel;
import pro.hirooka.chukasa.domain.chukasa.ChukasaSettings;
import pro.hirooka.chukasa.domain.chukasa.HTML5PlayerModel;
import pro.hirooka.chukasa.domain.chukasa.type.PlaylistType;
import pro.hirooka.chukasa.domain.chukasa.type.StreamingType;
import pro.hirooka.chukasa.handler.ChukasaRemover;
import pro.hirooka.chukasa.handler.ChukasaRemoverRunner;
import pro.hirooka.chukasa.handler.ChukasaStopper;
import pro.hirooka.chukasa.handler.ChukasaThreadHandler;
import pro.hirooka.chukasa.operator.IDirectoryCreator;
import pro.hirooka.chukasa.operator.ITimerTaskParameterCalculator;
import pro.hirooka.chukasa.service.chukasa.IChukasaModelManagementComponent;
import pro.hirooka.chukasa.transcoder.FFmpegInitializer;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("video")
public class HTML5PlayerController {

    static final String FILE_SEPARATOR = System.getProperty("file.separator");

    @Autowired
    ChukasaConfiguration chukasaConfiguration;
    @Autowired
    SystemConfiguration systemConfiguration;
    @Autowired
    HLSConfiguration hlsConfiguration;
    @Autowired
    IChukasaModelManagementComponent chukasaModelManagementComponent;
    @Autowired
    IDirectoryCreator directoryCreator;
    @Autowired
    ITimerTaskParameterCalculator timerTaskParameterCalculator;
    @Autowired
    ChukasaStopper chukasaStopper;
    @Autowired
    ChukasaRemover chukasaRemover;
    @Autowired
    HttpServletRequest httpServletRequest;

    @RequestMapping(method = RequestMethod.POST)
    String play(Model model, @Validated ChukasaSettings chukasaSettings, BindingResult bindingResult){

        if(bindingResult.hasErrors()){
            return "index";
        }

        // 再生前に FFmpeg, タイマー，ストリームをまっさらに．
        for(ChukasaModel chukasaModel : chukasaModelManagementComponent.get()){
            chukasaModel.getSegmenterRunner().stop();
            chukasaModel.getPlaylisterRunner().stop();
            FFmpegInitializer ffmpegInitializer = new FFmpegInitializer((long)chukasaModel.getFfmpegPID());
            Thread ffmpegInitializerThread = new Thread(ffmpegInitializer);
            ffmpegInitializerThread.start();
            ChukasaRemoverRunner chukasaRemoverRunner = new ChukasaRemoverRunner(systemConfiguration, chukasaModel.getStreamRootPath(), chukasaModel.getUuid());
            Thread thread = new Thread(chukasaRemoverRunner);
            thread.start();
        }
        chukasaModelManagementComponent.deleteAll();

        if(chukasaModelManagementComponent.get().size() > 0){
            log.error("chukasaModelManagementComponent.get.size() > 0");
        }else {

            log.info("ChukasaSettings -> {}", chukasaSettings.toString());

            boolean isNativeHlsSupported = false;
            String userAgent = httpServletRequest.getHeader("user-agent");
            if((userAgent.contains("Mac OS X 10_11") && (userAgent.contains("Version") && userAgent.split("Version/")[1].split(" ")[0].contains("9")))
                    || (userAgent.contains("iPhone OS 9") && (userAgent.contains("Version") && userAgent.split("Version/")[1].split(" ")[0].contains("9")))
                    || (userAgent.contains("iPad; CPU OS 9") && (userAgent.contains("Version") && userAgent.split("Version/")[1].split(" ")[0].contains("9")))
                    || (userAgent.contains("Windows") && userAgent.contains("Edge/"))){
                isNativeHlsSupported = true;
            }
            log.info("{} : {}", isNativeHlsSupported, userAgent);

            ChukasaModel chukasaModel = new ChukasaModel();

            // TODO: adaptive
            chukasaModel.setAdaptiveBitrateStreaming(0);

            chukasaModel.setChukasaConfiguration(chukasaConfiguration);
            chukasaModel.setSystemConfiguration(systemConfiguration);
            chukasaModel.setHlsConfiguration(hlsConfiguration);
            chukasaModel.setChukasaSettings(chukasaSettings);

            String encodingSettings = chukasaModel.getChukasaSettings().getEncodingSettingsType().getName();
            String videoResolution = encodingSettings.split("-")[0];
            int videoBitrate = Integer.parseInt(encodingSettings.split("-")[1]);
            int audioBitrate = Integer.parseInt(encodingSettings.split("-")[2]);
            chukasaModel.getChukasaSettings().setVideoResolution(videoResolution);
            chukasaModel.getChukasaSettings().setVideoBitrate(videoBitrate);
            chukasaModel.getChukasaSettings().setAudioBitrate(audioBitrate);

            String streamRootPath = httpServletRequest.getSession().getServletContext().getRealPath("") + chukasaConfiguration.getStreamRootPathName();
            streamRootPath = streamRootPath + FILE_SEPARATOR + chukasaModel.getUuid().toString();
            chukasaModel.setStreamRootPath(streamRootPath);

            chukasaModel = chukasaModelManagementComponent.create(0, chukasaModel);

            directoryCreator.setup(0);

            timerTaskParameterCalculator.calculate(0);

            ChukasaThreadHandler chukasaThreadHandler = new ChukasaThreadHandler(chukasaModel.getAdaptiveBitrateStreaming(), chukasaModelManagementComponent);
            Thread thread = new Thread(chukasaThreadHandler);
            thread.start();

            chukasaModel = chukasaModelManagementComponent.get(0);

            String playlistURI = "";
            if(chukasaModel.getChukasaSettings().getStreamingType() == StreamingType.WEB_CAMERA
                    || chukasaModel.getChukasaSettings().getStreamingType() == StreamingType.CAPTURE){
                playlistURI = "/"
                        + chukasaModel.getChukasaConfiguration().getStreamRootPathName()
                        + FILE_SEPARATOR
                        + chukasaModel.getUuid().toString()
                        + FILE_SEPARATOR
                        + chukasaModel.getChukasaConfiguration().getLivePathName()
                        + FILE_SEPARATOR
                        + chukasaModel.getChukasaSettings().getVideoBitrate()
                        + FILE_SEPARATOR
                        + chukasaModel.getChukasaConfiguration().getM3u8PlaylistName();
            }else if(chukasaModel.getChukasaSettings().getStreamingType() == StreamingType.FILE
                    || chukasaModel.getChukasaSettings().getStreamingType() == StreamingType.OKKAKE){
                playlistURI = "/"
                        + chukasaModel.getChukasaConfiguration().getStreamRootPathName()
                        + FILE_SEPARATOR
                        + chukasaModel.getUuid().toString()
                        + FILE_SEPARATOR
                        + chukasaModel.getChukasaSettings().getFileName()
                        + FILE_SEPARATOR
                        + chukasaModel.getChukasaSettings().getVideoBitrate()
                        + FILE_SEPARATOR
                        + chukasaModel.getChukasaConfiguration().getM3u8PlaylistName();
            }
            log.info(playlistURI);

            HTML5PlayerModel html5PlayerModel = new HTML5PlayerModel();
            html5PlayerModel.setPlaylistURI(playlistURI);
            model.addAttribute("html5PlayerModel", html5PlayerModel);

            if(isNativeHlsSupported){
                return "player";
            }else{
                return chukasaConfiguration.getAlternativeHlsPlayer() + "-player";
            }

        }

        return "index";
    }

    // TODO: GET 修正
    @RequestMapping(method = RequestMethod.GET)
    String play(Model model,
                @RequestParam StreamingType streamingtype,
                @RequestParam int ch,
                @RequestParam int videobitrate,
                @RequestParam int duration,
                @RequestParam boolean encrypted){

        if(chukasaModelManagementComponent.get().size() > 0){
            log.warn("cannot start streaming bacause previous one is not finished.");
        }else {

            Map<String, String> env = System.getenv();
            for (String name : env.keySet()) {
                log.info("{} {}", name, env.get(name));
                if (name.equals("HOME")) {
                    String filePath = systemConfiguration.getFilePath().replace("$HOME", env.get(name));
                    systemConfiguration.setFilePath(filePath);
                }
            }

            ChukasaSettings chukasaSettings = new ChukasaSettings();
            chukasaSettings.setAdaptiveBitrateStreaming(0);
            chukasaSettings.setStreamingType(streamingtype);
            chukasaSettings.setCh(ch);
            chukasaSettings.setVideoBitrate(videobitrate);
            //chukasaSettings.setVideoResolutionType(VideoResolutionType.HD);
            //chukasaSettings.setCaptureResolutionType(VideoResolutionType.HD);
            chukasaSettings.setAudioBitrate(128);
            chukasaSettings.setTotalWebCameraLiveduration(duration);
            chukasaSettings.setEncrypted(encrypted);

            log.info("ChukasaSettings -> {}", chukasaSettings.toString());

            ChukasaModel chukasaModel = new ChukasaModel();

            // TODO: adaptive
            chukasaModel.setAdaptiveBitrateStreaming(0);

            chukasaModel.setChukasaConfiguration(chukasaConfiguration);
            chukasaModel.setSystemConfiguration(systemConfiguration);
            chukasaModel.setHlsConfiguration(hlsConfiguration);
            chukasaModel.setChukasaSettings(chukasaSettings);
            chukasaModel.setPlaylistType(PlaylistType.EVENT);

            String streamRootPath = httpServletRequest.getSession().getServletContext().getRealPath("") + chukasaConfiguration.getStreamRootPathName();
            chukasaModel.setStreamRootPath(streamRootPath);

            chukasaModel = chukasaModelManagementComponent.create(0, chukasaModel);

            directoryCreator.setup(0);

            timerTaskParameterCalculator.calculate(0);

            ChukasaThreadHandler chukasaThreadHandler = new ChukasaThreadHandler(chukasaModel.getAdaptiveBitrateStreaming(), chukasaModelManagementComponent);
            Thread thread = new Thread(chukasaThreadHandler);
            thread.start();

            chukasaModel = chukasaModelManagementComponent.get(0);

            String playlistURI = "/"
                    + chukasaModel.getChukasaConfiguration().getStreamRootPathName()
                    + FILE_SEPARATOR
                    + chukasaModel.getChukasaConfiguration().getLivePathName()
                    + FILE_SEPARATOR
                    + chukasaModel.getChukasaSettings().getVideoBitrate()
                    + FILE_SEPARATOR
                    + chukasaModel.getChukasaConfiguration().getM3u8PlaylistName();
            log.info(playlistURI);

            HTML5PlayerModel html5PlayerModel = new HTML5PlayerModel();
            html5PlayerModel.setPlaylistURI(playlistURI);
            model.addAttribute("html5PlayerModel", html5PlayerModel);

            return "player";
        }
        return null;
    }

    @RequestMapping(value = "/stop", method = RequestMethod.GET)
    String stop(){
        chukasaStopper.stop();

        //return "redirect:";
        return "redirect:/video/remove";
    }

    @RequestMapping(value = "/remove", method = RequestMethod.GET)
    String remove(){

        if(chukasaModelManagementComponent.get().size() > 0){
            log.warn("cannot remove files bacause streaming is not finished.");
        }else {
            String streamRootPath = httpServletRequest.getSession().getServletContext().getRealPath("") + chukasaConfiguration.getStreamRootPathName();
            if(Files.exists(new File(streamRootPath).toPath())) {
                chukasaRemover.setStreamRootPath(streamRootPath);
                chukasaRemover.remove();
            }else {
                log.warn("cannot remove files bacause streamRootPath: {} does not exist.", streamRootPath);
            }
        }

        return "redirect:/";
    }
}
