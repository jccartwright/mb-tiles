package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest
class MultibeamRepositorySpec extends Specification {
    @Autowired
    MultibeamRepository repository

    def "check if survey exists in database"(surveyId, expected) {
        expect:
        repository.surveyExists(surveyId) == expected
        where:

        surveyId  | expected
        'NEW2602' | true
        'NONE'    | false
    }

    def "check that more surveys created in two years than in one"() {
        setup:
        def startDate =  Date.parse('yyyy-MM-dd', '2017-01-01')
        def endDate = Date.parse('yyyy-MM-dd', '2018-01-01')
        def surveys2017 = repository.getSurveysByDate(startDate, endDate).size()
        def surveysSince2017 = repository.getSurveysByDate(startDate).size()

        expect:
        surveys2017 > 0
        surveysSince2017 > surveys2017
    }


}
