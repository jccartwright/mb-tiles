package ncei.gis

import org.springframework.stereotype.Repository
import groovy.sql.Sql

@Repository
class MultibeamRepository {

    //    @Autowired
    //    JdbcTemplate jdbcTemplate;


    List getSurveys() {
        return []
    }

    String getSurveyById(String surveyId) {
        //throw Exception if count != 1
        return null
    }

    List getSurveysByDate(start, end) {
        return []
    }

    Map getSurveyExtent(String surveyId) {
        //TODO use JDBC template
        def query = '''select 
        sdo_geom.sdo_min_mbr_ordinate(shape, 1) minx, 
        sdo_geom.sdo_min_mbr_ordinate(shape, 2) miny,
        sdo_geom.sdo_max_mbr_ordinate(shape, 1) maxx,
        sdo_geom.sdo_max_mbr_ordinate(shape, 2) maxy 
        from mb.mbinfo_survey_tsql where ngdc_id = ?'''

        def surveyExtent = sql.firstRow(query, [surveyId])
        if (! surveyExtent) {
            throw new IllegalStateException("ERROR: survey ${surveyId} is not present in MB.MBINFO_SURVEY_TSQL and cannot be processed.")
        }
        return surveyExtent  //GroovyRowResult implements java.util.Map
    }


    List getSurveyFiles(List coords, String survey) {
        String bbox = "POLYGON((${coords[0]} ${coords[1]}, ${coords[2]} ${coords[1]}, ${coords[2]} ${coords[3]}, ${coords[0]} ${coords[3]}, ${coords[0]} ${coords[1]}))"

        def query = """select
            DATA_FILE, MBIO_FORMAT_ID
        from
            MB.MBINFO_FILE_TSQL a
        where
          SDO_RELATE(
            a.SHAPE,
            MDSYS.SDO_GEOMETRY(?, 8307),
            'mask=anyinteract querytype=window'
        ) = 'TRUE' and NGDC_ID = ? and MBIO_FORMAT_ID in (162, 58)
        order by DATA_FILE"""

        sql.rows(query, [bbox, survey])
    }


}


