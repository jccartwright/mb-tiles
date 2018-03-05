package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest
class MultibeamServiceSpec extends Specification {
    //@Autowired
    //MultibeamService service

    MultibeamRepository repository = Mock()
    MultibeamService service = new MultibeamService(repository)

    def "get potential tiles for survey"() {
        given:
        repository.getSurveyExtent('NEW2602') >> ['minx': 144, 'miny': -8.0, 'maxx': -157, 'maxy': 22]

        when:
        List tiles = service.getPotentialTiles('NEW2602')

        then:
        2 * service.calcTiles(!null)
        println tiles
    }



    def "round down by specified interval"(value, interval, expected) {
        expect:
        service.mroundDown(value, interval) == expected

        where:
        value | interval | expected
        -144  | 10       | -150
        144   | 10       | 140
        146   | 10       | 140
        0     | 10       | 0
        146   | 5        | 145
    }

    def "round up by specified interval"(value, interval, expected) {
        expect:
        service.mroundUp(value, interval) == expected

        where:
        value | interval | expected
        -144  | 10       | -140
        144   | 10       | 150
        146   | 10       | 150
        0     | 10       | 0
        144   | 5        | 145
    }

    def "calculate tiles for given bbox"(bbox, expectedCount) {
        given:
        List tiles = service.calcTiles(bbox)

        expect:
        tiles.size() == expectedCount

        where:
        bbox                                                     | expectedCount
        ['minx': 124, 'miny': 2.0, 'maxx': 144, 'maxy': 13]      | 6
        //'NEW2602' | ['minx': 144, 'miny': -8.0, 'maxx': -157, 'maxy': 22]    | 16
    }

}
