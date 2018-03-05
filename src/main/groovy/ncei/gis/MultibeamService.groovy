package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.slf4j.*
import groovy.util.logging.Slf4j
import groovy.io.FileType


@Slf4j
@Service
class MultibeamService {
    private final repository

    //@Autowired optional w/ single constructor
    MultibeamService(MultibeamRepository repository) {
        this.repository = repository
    }

    List calcSurveyTiles(String surveyId) {
        def CELLSIZE = 10
        def results = []

        def surveyExtent
        try {
            surveyExtent = repository.getSurveyExtent(surveyId)
            //println surveyExtent
        } catch (e) {
            //replace w/ log stmt
            println (e.getMessage())
            surveyExtent = []
        }

        def rangeX = surveyExtent.maxx - surveyExtent.minx
        def rangeY = surveyExtent.maxy - surveyExtent.miny
        //println "survey ${surveyId} extends ${rangeX} degrees longitude, ${rangeY} degrees latitude"
        if (rangeX > 180 || rangeY > 90) {
            println "WARNING: survey extent exceeds 1 hemisphere - may indicate a antimeridian crossing problem!"
        }

        //extent of survey rounded to 10 degrees
        def MINX = mroundDown(surveyExtent.minx, CELLSIZE)
        def MINY = mroundDown(surveyExtent.miny, CELLSIZE)
        def MAXX = mroundUp(surveyExtent.maxx, CELLSIZE)
        def MAXY = mroundUp(surveyExtent.maxy, CELLSIZE)

        //init bounding values
        def minx = MINX
        def maxx = MINX + CELLSIZE
        def miny = MINY
        def maxy = MINY + CELLSIZE

        while (maxy <= MAXY) {
            while (maxx <= MAXX) {
                //println "${minx}, ${miny}, ${maxx}, ${maxy}"
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
     * remove any files from previous runs related to the specified survey
     */
    def cleanupFiles(File outputDir, String surveyId) {
        log.debug "removing all files for survey ${surveyId}"
        outputDir.eachFileMatch FileType.FILES, ~/.*_${surveyId}_.*/, {
            it.delete()
        }
    }


    /**
     * return a list of 10-degree tiles covering the specified survey. Each tile is
     * list of integers in the format of minx, miny, maxx, maxy
     */
    List getPotentialTiles(String surveyId) {

        def results = []

        //get the extent of the survey's MBR
        Map surveyExtent = repository.getSurveyExtent(surveyId)

        if (surveyExtent.minx > surveyExtent.maxx) {
            //assume AM-crossing survey. split original bbox into two, one on either side of AM.
            println "survey ${surveyId} appears to cross the antimeridian"

            log.debug "survey ${surveyId} appears to cross the antimeridian"
            results += calcTiles([minx: surveyExtent.minx, miny: surveyExtent.miny, maxx: 180.0, maxy: surveyExtent.maxy])
            results += calcTiles([minx: -180, miny: surveyExtent.miny, maxx: surveyExtent.maxx, maxy: surveyExtent.maxy])
        } else {
            //assume minx, maxx in same hemisphere
            results = calcTiles(surveyExtent)
        }

        return results
    }


    /**
     * calculate list of tiles to cover the specified area.  Will return >1 tile if
     * survey exceeds CELLSIZE degrees in latitude or longitude
     */
    List calcTiles(coords) {
        println "inside calcTiles with ${coords}"
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
                //println "${minx}, ${miny}, ${maxx}, ${maxy}"
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


    Integer mroundUp(Double number, Integer multiple) {
        //round up
        def x = Math.ceil(number)
        //short-circuit if already a multiple
        if (x % multiple == 0) {
            return x
        }

        def m = Math.floor((x/multiple)+1)
        return (m * multiple)
    }


    Integer mroundDown(Double number, Integer multiple) {
        //round down
        def x = Math.floor(number)
        //short-circuit if already a multiple
        if (x % multiple == 0) {
            return x
        }

        def m = Math.floor((x/multiple))
        return (m * multiple)
    }
}
