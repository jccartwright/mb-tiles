package ncei.gis


import org.slf4j.*
import groovy.util.logging.Slf4j

@Slf4j
class InprocessProcessingOperations implements ProcessingOperations {
    File outputDir

    InprocessProcessingOperations(File outputDir) {
        this.outputDir = outputDir
    }

    void initSurvey(String survey) {
        log.debug "processing survey ${survey}..."
    }


    File createGrid(List coords, String survey, File surveyManifestFile) {
        def RESOLUTION = 0.00083333333
        def offset  = (RESOLUTION / 2)

        //output grid name is manifest name w/o extension
        def tileName = surveyManifestFile.name.replaceFirst(~/\.[^\.]+$/, '')

	    def startTime = new Date()

        String cmd = "mbgrid -A2 -C1 -E${RESOLUTION}/${RESOLUTION}/degrees -F2 -G3 -I${surveyManifestFile.name} -O${tileName}  -R${coords[0] + offset}/${coords[2] - offset}/${coords[1] + offset}/${coords[3] - offset}"
        def env = ["PATH=${execPath}"]
        log.debug "generating grid ${tileName}..."

        def process = cmd.execute(env, this.outputDir)
        process.waitFor()
        def stdErr = new File(this.outputDir, "${tileName}.stderr")
        stdErr.write(process.err.text)
        log.debug "stderr: ${process.err.text}"

        def stdOut = new File(this.outputDir, "${tileName}.stdout")
        stdOut.write("executing command:\n${cmd}\n\n")
        stdOut.append(process.in.text)
        log.debug "stdout: ${process.in.text}"

        if (process.exitValue()) {
            log.error "mbgrid exited with value ${process.exitValue()}"
        }
        log.debug "exitValue: ${process.exitValue()}"

        def endTime = new Date()
        def elapsedTime = endTime.time - startTime.time
        log.info "createGrid completed in ${formatMilliseconds(elapsedTime)}"
        stdOut.append("\n\ncommand completed in ${formatMilliseconds(elapsedTime)}")

        //verify NetCDF file actually created and return
        File gridFile = new File(outputDir, "${tileName}.grd")
         if (! gridFile.exists()) {
         	throw new Exception("grid file not found for survey ${survey}")
         }
        return gridFile
    }


    File createLERC(File gridFile) {
        String tileName = getFilenameWithoutExtension(gridFile)

        log.debug "creating LERC file for ${tileName}..."
        def cmd = """gdal_translate -of MRF -a_srs epsg:4326 -a_nodata 99999 -co COMPRESS=LERC -co OPTIONS="LERC_PREC=0.005" ${gridFile.name} ${tileName}.mrf"""
        def env = ["PATH=${execPath}"]
        log.debug cmd

        def process = cmd.execute(env, outputDir)
        process.waitFor()
        log.debug "stderr: ${process.err.text}"
        log.debug "stdout: ${process.in.text}"
        log.debug "exitValue: ${process.exitValue()}"
         if (process.exitValue()) {
         	log.error "gdal_translate exited with value ${process.exitValue()}"
         }

        File mrfFile = new File(outputDir, "${tileName}.mrf")
         if (! mrfFile.exists()) {
         	throw new Exception("LERC file not found for tile ${tileName}")
         }

        cmd = "gdaladdo -r avg ${mrfFile.name} 2 4 8 16 32 64"
        log.debug cmd
        process = cmd.execute(env, outputDir)
        process.waitFor()
        log.debug "stderr: ${process.err.text}"
        log.debug "stdout: ${process.in.text}"
        log.debug "exitValue: ${process.exitValue()}"
         if (process.exitValue()) {
         	println "ERROR: gdaladdo exited with value ${process.exitValue()}"
         }

        return mrfFile
    }


    String getFilenameWithoutExtension(File file) {
        return file.name.replaceFirst(~/\.[^\.]+$/, '')
    }


    String formatMilliseconds(Long ms) {
        Long totalSecs = ms / 1000
        Integer hours = totalSecs / 3600
        Integer minutes = (totalSecs % 3600) / 60
        Integer seconds = totalSecs % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


    void cleanupSurvey(String Survey) {
        //keep  .mrf, .lrc, .idx, .aux.xml
        //TODO "rm *.grd *.grd.cmd *.mb-1 *.mbf\n"
    }


}
