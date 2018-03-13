package ncei.gis

import groovy.io.FileType
import org.slf4j.*
import groovy.util.logging.Slf4j

@Slf4j
class BatchProcessingOperations implements ProcessingOperations {
    File outputDir
    File cmds

    BatchProcessingOperations(File outputDir, String batchFilename) {
        this.outputDir = outputDir
        cmds = new File(outputDir, batchFilename)

        //initialize file to which all commands are written
        if (cmds.exists()) {
            log.info "removing existing batch file ${cmds}"
            cmds.delete()
        }
        cmds.write "# batch file generated ${new Date()}\n"
    }

    void initSurvey(String survey) {
        log.debug "processing survey ${survey}..."
        cmds << "## survey ${survey} ##\n"
    }

    void cleanupSurvey(String survey) {
        //keep  .mrf, .lrc, .idx, .aux.xml
        cmds << "rm *.grd *.grd.cmd *.mb-1 *.mbf\n"
    }

    File createGrid(List coords, String survey, File surveyManifestFile) {
        def RESOLUTION = 0.00083333333
        def offset = (RESOLUTION / 2)

        //output grid name is manifest name w/o extension
        def tileName = surveyManifestFile.name.replaceFirst(~/\.[^\.]+$/, '')

        String cmd = "mbgrid -A2 -C1 -E${RESOLUTION}/${RESOLUTION}/degrees -F2 -G3 -I${surveyManifestFile.name} -O${tileName}  -R${coords[0] + offset}/${coords[2] - offset}/${coords[1] + offset}/${coords[3] - offset}"
        log.debug "generating grid ${tileName}..."
        cmds << "${cmd}\n"

        //no actual file created
        return new File(outputDir, "${tileName}.grd")
    }


    File createLERC(File gridFile) {
        def tileName = getFilenameWithoutExtension(gridFile)

        log.debug "creating LERC file for ${tileName}..."
        def cmd = """gdal_translate -of MRF -a_srs epsg:4326 -a_nodata 99999 -co COMPRESS=LERC -co OPTIONS="LERC_PREC=0.005" ${gridFile.name} ${tileName}.mrf"""
        cmds << "${cmd}\n"

        File mrfFile = new File(outputDir, "${tileName}.mrf")

        cmd = "gdaladdo -r avg ${mrfFile.name} 2 4 8 16 32 64"
        cmds << "${cmd}\n"

        //no actual file created
        return mrfFile
    }


    String getFilenameWithoutExtension(File file) {
        return file.name.replaceFirst(~/\.[^\.]+$/, '')
    }

}
