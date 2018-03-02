package ncei.gis

import org.slf4j.*
import groovy.util.logging.Slf4j

@Slf4j
class Tile {
    String survey
    String version
    String part //tiles may be split if number of files exceeds threshold
    List bbox
    File surveyFilesManifest  //${outputDir}/mbtile_${survey}_${coords[0]}_${coords[1]}_${counter}_v${version}.mbf

}
