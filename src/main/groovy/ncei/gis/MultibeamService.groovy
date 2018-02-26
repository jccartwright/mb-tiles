package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class MultibeamService {
    @Autowired
    MultibeamRepository repository

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
