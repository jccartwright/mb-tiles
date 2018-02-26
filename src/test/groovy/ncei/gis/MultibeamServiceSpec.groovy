package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest
class MultibeamServiceSpec extends Specification {
    @Autowired
    MultibeamService service

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

}
