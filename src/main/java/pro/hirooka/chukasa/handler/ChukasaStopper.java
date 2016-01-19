package pro.hirooka.chukasa.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import pro.hirooka.chukasa.domain.ChukasaModel;
import pro.hirooka.chukasa.domain.type.StreamingType;
import pro.hirooka.chukasa.service.IChukasaModelManagementComponent;
import pro.hirooka.chukasa.transcoder.FFmpegStopper;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
public class ChukasaStopper {

    private final IChukasaModelManagementComponent chukasaModelManagementComponent;

    @Autowired
    public ChukasaStopper(IChukasaModelManagementComponent chukasaModelManagementComponent) {
        this.chukasaModelManagementComponent = requireNonNull(chukasaModelManagementComponent, "chukasaModelManagementComponent");
    }

    public void stop(){

        log.info("stop command is called.");

        List<ChukasaModel> chukasaModelList = chukasaModelManagementComponent.get();

        // set flag timer cancel (segmenter, playlister)
        // if set flag true -> break while loop
        for(ChukasaModel chukasaModel : chukasaModelList){
            chukasaModel.setFlagTimerSegmenter(true);
            chukasaModel.setFlagTimerPlaylister(true);
            if(chukasaModel.getChukasaSettings().getStreamingType() == StreamingType.OKKAKE){
                chukasaModel.setFlagRemoveFile(true);
            }
            chukasaModelManagementComponent.update(chukasaModel.getAdaptiveBitrateStreaming(), chukasaModel);
        }

        FFmpegStopper ffmpegStopper = new FFmpegStopper(chukasaModelManagementComponent);
        Thread thread = new Thread(ffmpegStopper);
        thread.start();
    }
}
