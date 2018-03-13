package ncei.gis

import org.springframework.stereotype.Repository
import org.springframework.beans.factory.annotation.Autowired
import groovy.sql.Sql
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.beans.factory.annotation.Value
import org.slf4j.*
import groovy.util.logging.Slf4j



@Slf4j
@Repository
class MultibeamRepository {

    @Autowired
    JdbcTemplate jdbcTemplate


    Boolean surveyExists(String surveyId) {
        def result = jdbcTemplate.queryForList(
         'select NGDC_ID from MB.SURVEY where NGDC_ID = ?', surveyId
        )
        return (result.size() == 1)
    }


    List getSurveysByDate(Date start) {
        if (! start) {
            throw new IllegalArgumentException("start date must be specified")
        }

        List result = jdbcTemplate.queryForList('select NGDC_ID from MB.SURVEY where ENTERED_DATE >= ?', start)

        //hack to accommodate missing ENTERED_DATE values by using END_TIME as proxy
        result += jdbcTemplate.queryForList('select NGDC_ID from MB.SURVEY where ENTERED_DATE is null and END_TIME >= ?', start)
        return result['NGDC_ID']
    }


    List getSurveysByDate(Date start, Date end) {
        if (! end) {
            return getSurveysByDate(start)
        }

        //exclusive of end date
        List result = jdbcTemplate.queryForList('select NGDC_ID from MB.SURVEY where ENTERED_DATE between ? and ?', start, end)

        //hack to accommodate missing ENTERED_DATE values by using END_TIME as proxy
        result += jdbcTemplate.queryForList('select NGDC_ID from MB.SURVEY where ENTERED_DATE is null and END_TIME between ? and ?', start, end)

        return result['NGDC_ID']
    }


    Map getSurveyExtent(String surveyId) {
        Map surveyExtent = jdbcTemplate.queryForMap('''select 
        sdo_geom.sdo_min_mbr_ordinate(shape, 1) minx, 
        sdo_geom.sdo_min_mbr_ordinate(shape, 2) miny,
        sdo_geom.sdo_max_mbr_ordinate(shape, 1) maxx,
        sdo_geom.sdo_max_mbr_ordinate(shape, 2) maxy 
        from mb.mbinfo_survey_tsql where ngdc_id = ?''', surveyId)

        //check for valid coordinates
        if (surveyExtent.maxy > 90 || surveyExtent.miny < -90 || surveyExtent.miny > surveyExtent.maxy) {
            throw new IllegalStateException("survey has invalid latitude values and cannot be processed. miny=${surveyExtent.miny}; maxy=${surveyExtent.maxy}")
        }

        //minx may be greater than maxx when survey crosses the antimeridian
        if (surveyExtent.maxx > 180 || surveyExtent.minx < -180) {
            thrown new IllegalStateException("survey has invalid longitude values and cannot be processed. minx=${surveyExtent.minx}; maxx=${surveyExtent.maxx}")
        }

        return surveyExtent  //org.springframework.util.LinkedCaseInsensitiveMap
    }


    /**
     * return a list of all survey files which fall w/in the specified area and have the specified survey identifier
     */
    List getSurveyFiles(List coords, String survey) {
        String bbox = "POLYGON((${coords[0]} ${coords[1]}, ${coords[2]} ${coords[1]}, ${coords[2]} ${coords[3]}, ${coords[0]} ${coords[3]}, ${coords[0]} ${coords[1]}))"

        String query = '''select
            DATA_FILE, MBIO_FORMAT_ID
        from
            MB.MBINFO_FILE_TSQL a
        where
          SDO_RELATE(
            a.SHAPE,
            MDSYS.SDO_GEOMETRY(?, 8307),
            'mask=anyinteract querytype=window'
        ) = 'TRUE' and NGDC_ID = ? and MBIO_FORMAT_ID in (162, 58)
        order by DATA_FILE'''

        List results = jdbcTemplate.queryForList(query, bbox, survey)

        if (results.size() == 0) {
            log.info "no survey files found for survey ${survey} in area ${bbox}"
            //throw new Exception("no survey files found for survey ${survey} in area ${coords}")
        } else {
            log.debug "${results.size()} files found for survey ${survey} in area ${bbox}"
        }
        return results
    }
}


