package pro.hirooka.chukasa.detector;

import lombok.extern.slf4j.Slf4j;
import pro.hirooka.chukasa.domain.chukasa.ChukasaModel;
import pro.hirooka.chukasa.service.chukasa.IChukasaModelManagementComponent;

import java.util.Timer;

import static java.util.Objects.requireNonNull;

@Slf4j
public class FFmpegHLSStreamDetectorRunner implements Runnable {

    private int adaptiveBitrateStreaming;
    private boolean isRunning = true;

    private IChukasaModelManagementComponent chukasaModelManagementComponent;

    public FFmpegHLSStreamDetectorRunner(int adaptiveBitrateStreaming, IChukasaModelManagementComponent chukasaModelManagementComponent){
        this.adaptiveBitrateStreaming = adaptiveBitrateStreaming;
        this.chukasaModelManagementComponent = requireNonNull(chukasaModelManagementComponent, "chukasaModelManagementComponent");
    }

    @Override
    public void run() {

        Timer detectorTimer = new Timer();
        detectorTimer.scheduleAtFixedRate(new FFmpegHLSStreamDetector(adaptiveBitrateStreaming, chukasaModelManagementComponent), 0, 1000);

        while(isRunning){
            ChukasaModel chukasaModel = chukasaModelManagementComponent.get(adaptiveBitrateStreaming);
            if(chukasaModel == null || chukasaModel.isFlagTimerSegmenter()){
                detectorTimer.cancel();
                log.info("{} is completed.", this.getClass().getName());
                break;
            }
        }
        if(!isRunning){
            detectorTimer.cancel();
        }
    }
}
