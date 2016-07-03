package pro.hirooka.chukasa.detector;

import lombok.extern.slf4j.Slf4j;
import pro.hirooka.chukasa.ChukasaConstant;
import pro.hirooka.chukasa.domain.chukasa.ChukasaModel;
import pro.hirooka.chukasa.domain.chukasa.type.PlaylistType;
import pro.hirooka.chukasa.service.chukasa.IChukasaModelManagementComponent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import static java.util.Objects.requireNonNull;

@Slf4j
public class FFmpegHLSStreamDetector extends TimerTask {

    static final String FILE_SEPARATOR = System.getProperty("file.separator");

    final String STREAM_FILE_NAME_PREFIX = ChukasaConstant.STREAM_FILE_NAME_PREFIX;
    final String STREAM_FILE_EXTENSION = ChukasaConstant.STREAM_FILE_EXTENSION;
    final String FFMPEG_HLS_M3U8_FILE_NAME = ChukasaConstant.FFMPEG_HLS_M3U8_FILE_NAME;
    final String M3U8_FILE_EXTENSION = ChukasaConstant.M3U8_FILE_EXTENSION;

    private int adaptiveBitrateStreaming;

    private IChukasaModelManagementComponent chukasaModelManagementComponent;

    FFmpegHLSStreamDetector(int adaptiveBitrateStreaming, IChukasaModelManagementComponent chukasaModelManagementComponent) {
        this.adaptiveBitrateStreaming = adaptiveBitrateStreaming;
        this.chukasaModelManagementComponent = requireNonNull(chukasaModelManagementComponent, "chukasaModelManagementComponent");
    }

    @Override
    public void run() {

        ChukasaModel chukasaModel = chukasaModelManagementComponent.get(adaptiveBitrateStreaming);
        int sequenceTS = chukasaModel.getSeqTs();
        String streamPath = chukasaModel.getStreamPath();
        log.info("sequenceTS = {}", sequenceTS);
        log.debug("streamPath = {}", streamPath);

        String tsPath = streamPath + FILE_SEPARATOR + STREAM_FILE_NAME_PREFIX + (sequenceTS + 1) + STREAM_FILE_EXTENSION;
        File file = new File(tsPath);
        if(file.exists()){
            log.debug("file exists: {}", file.getAbsolutePath());
            tsPath = streamPath + FILE_SEPARATOR + STREAM_FILE_NAME_PREFIX + (sequenceTS + 2) + STREAM_FILE_EXTENSION;
            file = new File(tsPath);
            if(file.exists()) {
                log.debug("file exists: {}", file.getAbsolutePath());
                sequenceTS = sequenceTS + 1;
                chukasaModel.setSeqTs(sequenceTS);

                // FFmpeg のプレイリストから EXTINF を読み取るのはいったんやめるです．強制的に値を設定するです．
//                List<Double> extinfList = chukasaModel.getExtinfList();
//                List<Double> ffmpegM3U8EXTINFList = getEXTINFList(streamPath + FILE_SEPARATOR + FFMPEG_HLS_M3U8_FILE_NAME + M3U8_FILE_EXTENSION);
//                if(sequenceTS >= 0 && ffmpegM3U8EXTINFList.size() > 0){
//                    extinfList.add(ffmpegM3U8EXTINFList.get(sequenceTS));
//                }else{
//                    extinfList.add((double)chukasaModel.getHlsConfiguration().getDuration());
//                }
//                chukasaModel.setExtinfList(extinfList);
                chukasaModel.getExtinfList().add((double)chukasaModel.getHlsConfiguration().getDuration());

                chukasaModelManagementComponent.update(adaptiveBitrateStreaming, chukasaModel);

                // LIVE プレイリストの場合は過去の不要なファイルを削除する．
                if(chukasaModel.getChukasaSettings().getPlaylistType().equals(PlaylistType.LIVE)) {
                    int sequencePlaylist = chukasaModel.getSeqPl();
                    int URI_IN_PLAYLIST = chukasaModel.getHlsConfiguration().getUriInPlaylist();
                    for (int i = 0; i < sequencePlaylist - URI_IN_PLAYLIST; i++) {
                        File oldTSFile = new File(streamPath + FILE_SEPARATOR + STREAM_FILE_NAME_PREFIX + i + STREAM_FILE_EXTENSION);
                        if (oldTSFile.exists()) {
                            oldTSFile.delete();
                        }
                    }
                }
            }
        }
    }

    private List<Double> getEXTINFList(String m3u8Path){
        File m3u8 = new File(m3u8Path);
        if(m3u8.exists()) try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(m3u8));
            final String EXTINF_TAG = "#EXTINF:";
            List<Double> extinfList = new ArrayList<>();
            String str = "";
            while ((str = bufferedReader.readLine()) != null) {
                if (str.startsWith(EXTINF_TAG)) {
                    try {
                        double extinf = Double.parseDouble(str.split(EXTINF_TAG)[1].split(",")[0]);
                        log.debug("extinf = {}", extinf);
                        extinfList.add(extinf);
                    }catch(NumberFormatException e){
                        log.error("{} {}", e.getMessage(), e);
                        break;
                    }
                }
            }
            return extinfList;
        } catch (IOException e) {
            log.error("{} {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }
}
