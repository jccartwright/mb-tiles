package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.slf4j.*
import groovy.util.logging.Slf4j
import groovy.io.FileType
import groovy.json.*


@Slf4j
@Service
class MultibeamService {
    File outputDir
    def slurper = new JsonSlurper()

    /**
     * list of survey files w/in the specified tile formatted suitably for mbgrid.
     * coords define tile boundary.
     */
    List generateSurveyFileManifest(List coords, String survey, List files) {
        def MAX_SIZE = 75

        //create a list of files for each version
        def versions = [:]
        files.each { row ->
            //WARNING: this is brittle and depends on file path convention
            def version = row.DATA_FILE.split('/')[6]

            if (! versions[(version)]) {
                versions[(version)] = []
            }
            versions[(version)].push(row)
        }
        // versions.each {k,v ->
        // 	println("${k}: ${versions[(k)].size()}")
        // }

        //one manifest file for each version of each tile. Additional files if size exceeeds MAX_SIZE entries
        List mbfFiles = []
        versions.each {k,v ->
            //WARNING: depends on naming convention of version[n], e.g. "version1"
            def version = k[7]
            def filesVersioned = versions[k]

            //split original list of survey files into List of Lists, each with <= MAX_SIZE elements
            def parts = filesVersioned.collate(MAX_SIZE)
            def counter = 0
            parts.each { part ->

                def fileName = "mbtile_${survey}_${coords[0]}_${coords[1]}_${counter}_v${version}.mbf"
                def mbfFile = new File(outputDir, fileName)

                log.debug "generating mbfFile ${mbfFile}..."
                part.each {
                    def filename = it.DATA_FILE
                    if (filename.endsWith('.gz')) {
                        //*.gz implies presence of fbt file and that is preferred
                        filename = filename.substring (0, filename.lastIndexOf(".gz"))
                        mbfFile << "/mgg/MB/${filename}.fbt 71\n"    //"71" is FBT
                    } else {
                        mbfFile << "/mgg/MB/${filename} ${it.MBIO_FORMAT_ID}\n"
                    }
                }
                mbfFiles.push(mbfFile)
                counter++
            }
        }

        return mbfFiles
    }


    /**
     * remove any files from previous runs related to the specified survey
     */
    def cleanupOutputDir(String surveyId) {
        log.debug "removing all files for survey ${surveyId}"
        outputDir.eachFileMatch FileType.FILES, ~/.*_${surveyId}_.*/, {
            it.delete()
        }
    }


    /**
     * return a list of 10-degree tiles covering the specified survey. Each tile is
     * list of integers in the format of minx, miny, maxx, maxy
     */
    List getPotentialTiles(Map surveyExtent) {

        def results = []

        if (extentCrossesAntimeridian(surveyExtent)) {
            //split original extent into two, one on either side of AM.
            log.debug "given extent appears to cross the antimeridian"
            results += calcTiles([minx: surveyExtent.minx, miny: surveyExtent.miny, maxx: 180.0, maxy: surveyExtent.maxy])
            results += calcTiles([minx: -180, miny: surveyExtent.miny, maxx: surveyExtent.maxx, maxy: surveyExtent.maxy])
        } else {
            //assume minx, maxx in same hemisphere
            results = calcTiles(surveyExtent)
        }
        return results
    }


    Boolean extentCrossesAntimeridian(extent) {
        return (extent.minx > extent.maxx)
    }


    /**
     * calculate list of tiles to cover the specified area.  Will return >1 tile if
     * survey exceeds CELLSIZE degrees in latitude or longitude
     */
    List calcTiles(coords) {
        //TODO throw exception if extent crosses AM?

        def CELLSIZE = 10
        List results = []

        //extent of survey rounded to 10 degrees
        def MINX = mroundDown(coords.minx, CELLSIZE)
        def MINY = mroundDown(coords.miny, CELLSIZE)
        def MAXX = mroundUp(coords.maxx, CELLSIZE)
        def MAXY = mroundUp(coords.maxy, CELLSIZE)

        //init bounding values
        def minx = MINX
        def maxx = MINX + CELLSIZE
        def miny = MINY
        def maxy = MINY + CELLSIZE

        while (maxy <= MAXY) {
            while (maxx <= MAXX) {
                results.push([ minx, miny, maxx, maxy])

                minx = maxx
                maxx = maxx + CELLSIZE
            }

            minx = MINX
            maxx = MINX + CELLSIZE
            miny = maxy
            maxy = maxy + CELLSIZE
        }

        return results
    }


    /**
     * report on grids in the output directory
     *
     * TODO: write to separate file rather than to log
     */
    void gridReport() {
        outputDir.eachFileMatch FileType.FILES, ~/.*.mrf/, { gridFile ->
            try {
                Map depthRange = getDepthRange(gridFile)
                log.info "gridFile ${gridFile.name}: depth ranges from ${depthRange.min} to ${depthRange.max}"
            } catch (e) {
                log.warn "Error getting depth range for gridFile ${gridFile.name}: depth ranges from ${depthRange.min} to ${depthRange.max}"
            }
        }
    }


    Map getDepthRange(File gridFile) {
        def json = slurper.parseText("gdalinfo -json -stats ${gridFile.name}".execute([], outputDir).text)
        //gdal formats the JSON a little weird.
        Double min = new Double(json.bands.metadata[""].STATISTICS_MINIMUM[0]).round(2)
        Double max = new Double(json.bands.metadata[""].STATISTICS_MAXIMUM[0]).round(2)
        return ['min':min, 'max':max]
    }


    // mutates the provided list to remove non-existent files
    void removeNonexistentFiles(List surveyFiles) {
        println surveyFiles
        surveyFiles.removeAll {
            String filename = it.split()[0]
            File file = new File(filename)
            if (! file.exists()) {
                log.warn "Survey file ${file} not found - removing from survey file list"
                return true
            }
            //leave in list
            return false
        }
        println surveyFiles
    }


    /**
     * round up by specified interval
     */
    Integer mroundUp(Double value, Integer interval) {
        //short-circuit if already a multiple
        if (value % interval == 0) { return value }

        return ( interval * Math.ceil( value / interval ))
    }

    /**
     * round up by the specified interval
     */
    Integer mroundDown(Double value, Integer interval) {
        //short-circuit if already a multiple
        if (value % interval == 0) { return value }

        return ( interval * Math.floor ( value / interval ))
    }
}
