package pro.hirooka.chukasa.domain.service.chukasa.transcoder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pro.hirooka.chukasa.domain.model.chukasa.ChukasaModel;
import pro.hirooka.chukasa.domain.model.chukasa.enums.HardwareAccelerationType;
import pro.hirooka.chukasa.domain.service.chukasa.IChukasaModelManagementComponent;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.Future;

import static pro.hirooka.chukasa.domain.model.chukasa.constants.ChukasaConstant.*;
import static pro.hirooka.chukasa.domain.model.chukasa.constants.ChukasaConstant.FILE_SEPARATOR;

@Slf4j
@Service
public class FFmpegAndRecxxxService implements IFFmpegAndRecxxxService {

    private final IChukasaModelManagementComponent chukasaModelManagementComponent;

    @Autowired
    public FFmpegAndRecxxxService(IChukasaModelManagementComponent chukasaModelManagementComponent){
        this.chukasaModelManagementComponent = chukasaModelManagementComponent;
    }

    @Async
    @Override
    public Future<Integer> submit(int adaptiveBitrateStreaming) {

        // TODO: final
        ChukasaModel chukasaModel = chukasaModelManagementComponent.get(adaptiveBitrateStreaming);
        log.debug("StreamPath: {}", chukasaModel.getStreamPath());

        final HardwareAccelerationType hardwareAccelerationType = chukasaModel.getVideoCodecType();

        final boolean canEncrypt = chukasaModel.getChukasaSettings().isCanEncrypt();
        final String ffmpegOutputPath;
        if(canEncrypt){
            ffmpegOutputPath = chukasaModel.getTempEncPath() + FILE_SEPARATOR + STREAM_FILE_NAME_PREFIX + "%d" + STREAM_FILE_EXTENSION;
        } else {
            ffmpegOutputPath = chukasaModel.getStreamPath() + FILE_SEPARATOR + STREAM_FILE_NAME_PREFIX + "%d" + STREAM_FILE_EXTENSION;
        }

        final String[] commandArray;

        if(hardwareAccelerationType == HardwareAccelerationType.H264_OMX){
            commandArray = new String[]{
                    chukasaModel.getSystemConfiguration().getRecpt1Path(),
                    "--b25", "--strip",
                    Integer.toString(chukasaModel.getChukasaSettings().getPhysicalLogicalChannel()),
                    "-", "-",
                    "|",
                    chukasaModel.getSystemConfiguration().getFfmpegPath(),
                    "-i", "-",
                    "-acodec", "copy",
                    //"-acodec", "aac",
                    //"-ab", chukasaModel.getChukasaSettings().getAudioBitrate() + "k",
                    //"-ar", "44100",
                    //"-ac", "2",
                    //"-s", chukasaModel.getChukasaSettings().getVideoResolution(),
                    "-c:v", "h264_omx",
                    //"-vcodec", "h264_qsv",
                    //"-g", "60",
                    //"-profile:v", "high",
                    //"-level", "4.2",
                    //"-b:v", chukasaModel.getChukasaSettings().getVideoBitrate()+"k",
                    "-threads", Integer.toString(chukasaModel.getSystemConfiguration().getFfmpegThreads()),
                    "-f", "segment",
                    "-segment_format", "mpegts",
                    "-segment_time", Integer.toString(chukasaModel.getHlsConfiguration().getDuration()),
//                    "-segment_list", m3u8OutputPath,
                    ffmpegOutputPath
            };
        } else if(hardwareAccelerationType == HardwareAccelerationType.H264_QSV){
            commandArray = new String[]{
                    chukasaModel.getSystemConfiguration().getRecpt1Path(),
                    "--b25", "--strip",
                    Integer.toString(chukasaModel.getChukasaSettings().getPhysicalLogicalChannel()),
                    "-", "-",
                    "|",
                    chukasaModel.getSystemConfiguration().getFfmpegPath(),
                    "-i", "-",
                    "-acodec", "aac",
                    "-ab", chukasaModel.getChukasaSettings().getAudioBitrate() + "k",
                    "-ar", "44100",
                    "-ac", "2",
                    "-s", chukasaModel.getChukasaSettings().getVideoResolution(),
                    "-vcodec", "h264_qsv",
                    "-g", "60",
                    "-profile:v", "high",
                    "-level", "4.2",
                    "-b:v", chukasaModel.getChukasaSettings().getVideoBitrate()+"k",
                    "-threads", Integer.toString(chukasaModel.getSystemConfiguration().getFfmpegThreads()),
                    "-f", "segment",
                    "-segment_format", "mpegts",
                    "-segment_time", Integer.toString(chukasaModel.getHlsConfiguration().getDuration()),
//                    "-segment_list", m3u8OutputPath,
                    ffmpegOutputPath
            };
        }else if(hardwareAccelerationType == HardwareAccelerationType.H264){
            commandArray = new String[]{
                    chukasaModel.getSystemConfiguration().getRecpt1Path(),
                    "--b25", "--strip",
                    Integer.toString(chukasaModel.getChukasaSettings().getPhysicalLogicalChannel()),
                    "-", "-",
                    "|",
                    chukasaModel.getSystemConfiguration().getFfmpegPath(),
                    "-i", "-",
                    "-acodec", "aac",
                    "-ab", chukasaModel.getChukasaSettings().getAudioBitrate() + "k",
                    "-ar", "44100",
                    "-ac", "2",
                    "-s", chukasaModel.getChukasaSettings().getVideoResolution(),
                    "-vcodec", "libx264",
                    "-profile:v", "high",
                    "-level", "4.1",
                    "-b:v", chukasaModel.getChukasaSettings().getVideoBitrate()+"k",
                    "-preset:v", "superfast",
                    "-threads", Integer.toString(chukasaModel.getSystemConfiguration().getFfmpegThreads()),
                    "-f", "segment",
                    "-segment_format", "mpegts",
                    "-segment_time", Integer.toString(chukasaModel.getHlsConfiguration().getDuration()),
//                    "-segment_list", m3u8OutputPath,
                    "-x264opts", "keyint=10:min-keyint=10",
                    ffmpegOutputPath
            };
        } else {
            commandArray = new String[]{};
        }

        String command = "";
        for(int i = 0; i < commandArray.length; i++){
            command += commandArray[i] + " ";
        }
        log.info("{}", command);

        final String captureShell = chukasaModel.getSystemConfiguration().getTemporaryPath() + FILE_SEPARATOR + "capture.sh";
        File file = new File(captureShell);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            bufferedWriter.write("#!/bin/bash");
            bufferedWriter.newLine();
            bufferedWriter.write(command);
        } catch (IOException e) {
            log.error("{} {}", e.getMessage(), e);
        }

        // chmod 755 capture.sh
        if(true){
            final String[] chmodCommandArray = {"chmod", "755", captureShell};
            final ProcessBuilder processBuilder = new ProcessBuilder(chmodCommandArray);
            try {
                final Process process = processBuilder.start();
                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String str = "";
                while((str = bufferedReader.readLine()) != null){
                    log.debug("{}", str);
                }
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
                bufferedReader.close();
                process.destroy();
            } catch (IOException e) {
                log.error("{} {}", e.getMessage(), e);
            }
        }

        // run capture.sh
        if(true){
            final String[] capureCommandArray = {captureShell};
            final ProcessBuilder processBuilder = new ProcessBuilder(capureCommandArray);
            try {
                final Process process = processBuilder.start();
                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                long pid = -1;
                try {
                    if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                        final Field field = process.getClass().getDeclaredField("pid");
                        field.setAccessible(true);
                        pid = field.getLong(process);
                        chukasaModel.setFfmpegPID(pid);
                        chukasaModel = chukasaModelManagementComponent.update(adaptiveBitrateStreaming, chukasaModel);
                        field.setAccessible(false);
                    }
                } catch (Exception e) {
                    log.error("{} {}", e.getMessage(), e);
                }

                String str = "";
                boolean isTranscoding = false;
                while((str = bufferedReader.readLine()) != null){
                    log.debug("{}", str);
                    if(str.startsWith("frame=")){
                        if(!isTranscoding){
                            isTranscoding = true;
                            chukasaModel.setTrascoding(isTranscoding);
                            chukasaModel = chukasaModelManagementComponent.update(adaptiveBitrateStreaming, chukasaModel);
                        }
                    }
                }
                isTranscoding = false;
                chukasaModel.setTrascoding(isTranscoding);
                chukasaModelManagementComponent.update(adaptiveBitrateStreaming, chukasaModel);
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
                bufferedReader.close();
                process.destroy();
            } catch (IOException e) {
                log.error("{} {}", e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public void execute(int adaptiveBitrateStreaming) {

    }

    @Override
    public void cancel(int adaptiveBitrateStreaming) {

    }
}
