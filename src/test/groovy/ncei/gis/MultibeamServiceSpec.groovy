package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest
class MultibeamServiceSpec extends Specification {
    //@Autowired
    //MultibeamService service

    //MultibeamRepository repository = Mock()
    MultibeamService service = new MultibeamService()

    def "get potential tiles when survey crosses antimeridian"(Map extent, Integer expectedCount) {
        when:
        def tiles = service.getPotentialTiles(extent)
        println tiles


        then:
        tiles.size() == expectedCount


        where:
        extent || expectedCount
        ['minx': 170, 'miny': -10.0, 'maxx': -170, 'maxy': 10] || 4
        ['minx': 144, 'miny': -8.0, 'maxx': -157, 'maxy': 22] || 28

    }



    def "round down by specified interval"(value, interval, expected) {
        expect:
        service.mroundDown(value, interval) == expected

        where:
        value  | interval | expected
        -144.5   | 10       | -150
        -145   | 10       | -150
        -146   | 10       | -150
        -140.1 | 10      | -150
        144   | 10       | 140
        146   | 10       | 140
        0     | 10       | 0
        146   | 5        | 145
        170   | 10       | 170
        -170  | 10       | -170
    }

    def "round up by specified interval"(value, interval, expected) {
        expect:
        service.mroundUp(value, interval) == expected

        where:
        value | interval | expected
        -144.5  | 10       | -140
        -145    | 10       | -140
        144   | 10       | 150
        146   | 10       | 150
        0     | 10       | 0
        144   | 5        | 145
        -170  | 10       | -170
        170   | 10       | 170
    }

    def "calculate tiles for given bbox"(bbox, expectedCount) {
        given:
        List tiles = service.calcTiles(bbox)

        expect:
        tiles.size() == expectedCount

        where:
        bbox                                                     | expectedCount
        //['minx': 124, 'miny': 2.0, 'maxx': 144, 'maxy': 13]      | 6
        ['minx': -180, 'miny': -10.0, 'maxx': -170, 'maxy': 10]      | 2
        ['minx': 170, 'miny': -10.0, 'maxx': 180, 'maxy': 10]      | 2
    }


    def "remove nonexistent files"() {
        given:
        List fileList = ['README.md', 'build.gradle', 'nosuchfile']
        List expectedFileList = ['README.md', 'build.gradle']

        when:
        service.removeNonexistentFiles(fileList)

        then:
        expectedFileList.equals(fileList)
    }

}
