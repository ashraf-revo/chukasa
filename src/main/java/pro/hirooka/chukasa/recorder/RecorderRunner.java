package pro.hirooka.chukasa.recorder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import pro.hirooka.chukasa.configuration.SystemConfiguration;
import pro.hirooka.chukasa.domain.ReservedProgram;

import java.io.*;
import java.util.Date;

@Slf4j
public class RecorderRunner implements Runnable {

    static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private SystemConfiguration systemConfiguration;

    private ReservedProgram reservedProgram;

    public RecorderRunner(SystemConfiguration systemConfiguration, ReservedProgram reservedProgram){
        this.systemConfiguration = systemConfiguration;
        this.reservedProgram = reservedProgram;
    }

    @Override
    public void run() {

        int ch = reservedProgram.getCh();
        long begin = reservedProgram.getBegin();
        long start = reservedProgram.getStart();
        long end = reservedProgram.getEnd();
        int duration = reservedProgram.getDuration();
        String title = reservedProgram.getTitle();

        Date date = new Date();
        long now = date.getTime();

        if(now > start){
            // start recording immediately
            // Create do-record.sh (do-record_ch_yyyyMMdd_yyyyMMdd.sh)
            String doRecordFileName = "do-record_" + ch + "_" + begin + "_" + end + ".sh";
            try{
                File doRecordFile = new File(systemConfiguration.getFilePath() + FILE_SEPARATOR + doRecordFileName);
                if (!doRecordFile.exists()) {
                    doRecordFile.createNewFile();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(doRecordFile));
                    bw.write("#!/bin/bash");
                    bw.newLine();
                    bw.write(systemConfiguration.getCaptureProgramPath() + " --b25 --strip " + ch + " " + duration + " \"" + systemConfiguration.getFilePath() + FILE_SEPARATOR + ch + "_" + title + "_" + start + ".ts\"" + " >/dev/null");
                    bw.close();
                }

                String[] chmod = {"chmod", "755", systemConfiguration.getFilePath() + FILE_SEPARATOR + doRecordFileName};
                ProcessBuilder chmodProcessBuilder = new ProcessBuilder(chmod);
                Process chmodProcess = chmodProcessBuilder.start();
                InputStream chmodInputStream = chmodProcess.getErrorStream();
                InputStreamReader chmodInputStreamReader = new InputStreamReader(chmodInputStream);
                BufferedReader chmodBufferedReader = new BufferedReader(chmodInputStreamReader);
                String chmodString = "";
                while ((chmodString = chmodBufferedReader.readLine()) != null){
                    log.info(chmodString);
                }
                chmodBufferedReader.close();
                chmodInputStreamReader.close();
                chmodInputStream.close();
                chmodProcess.destroy();

                String[] run = {systemConfiguration.getFilePath() + FILE_SEPARATOR + doRecordFileName};
                ProcessBuilder runProcessBuilder = new ProcessBuilder(run);
                Process runProcess = runProcessBuilder.start();
                InputStream runInputStream = runProcess.getErrorStream();
                InputStreamReader runInputStreamReader = new InputStreamReader(runInputStream);
                BufferedReader runBufferedReader = new BufferedReader(runInputStreamReader);
                String runString = "";
                while ((runString = runBufferedReader.readLine()) != null){
                    log.info(runString);
                }
                runBufferedReader.close();
                runInputStreamReader.close();
                runInputStream.close();
                runProcess.destroy();

            }catch(IOException e){
                log.error("cannot run do-record.sh");
            }
        }

    }
}
