/*
 * Copyright (c) 2013-2015, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2015, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.util

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class DurationTest extends Specification {

    final long SEC = 1000
    final long MIN = 60 * SEC
    final long HOUR = 60 * MIN
    final long DAY = 24 * HOUR

    def 'test create by string' () {

        expect:
        Duration.of('123 millis').toMillis() == 123
        Duration.of('123 ms').toMillis() == 123
        Duration.of('123ms').toMillis() == 123

        Duration.of('5 seconds').toSeconds() == 5
        Duration.of('5 second').toSeconds() == 5
        Duration.of('5 sec').toSeconds() == 5
        Duration.of('5 s').toSeconds() == 5
        Duration.of('5s').toSeconds() == 5

        Duration.of('5 minutes').toSeconds() == 300
        Duration.of('5 minute').toSeconds() == 300
        Duration.of('5 min').toSeconds() == 300
        Duration.of('5min').toSeconds() == 300

        Duration.of('5 hours').toMinutes() == 300
        Duration.of('5 hour').toMinutes() == 300
        Duration.of('5 h').toMinutes() == 300
        Duration.of('5h').toMinutes() == 300

        Duration.of('1 days').toHours() == 24
        Duration.of('1 day').toHours() == 24
        Duration.of('1 d').toHours() == 24
        Duration.of('1d').toHours() == 24
        Duration.of('1days').toHours() == 24
        Duration.of('1day').toHours() == 24
        Duration.of('1d').toHours() == 24

    }

    def 'should not be parsed as a duration' () {
        when:
        new Duration('live_in_3d')
        then:
        thrown(IllegalArgumentException)
    }

    def 'should parse multi unit time format'() {
        expect:
        Duration.of('1d 2h').toMillis() ==  DAY + 2 * HOUR
        Duration.of('1 d 2 h').toMillis() ==  DAY + 2 * HOUR
        Duration.of('2d3h4m').toMillis() ==  2 * DAY + 3 * HOUR + 4 * MIN
        Duration.of('2d 3h 4m').toMillis() ==  2 * DAY + 3 * HOUR + 4 * MIN
    }

    def 'should parse legacy time format string' () {

        expect:
        Duration.of('1:0:0').toString() == '1h'
        Duration.of('01:00:00').toString() == '1h'
        Duration.of('10:00:00').toString() == '10h'
        Duration.of('01:02:03').toString() == '1h 2m 3s'
    }


    def 'test format' () {

        when:
        def duration = new Duration('5min')

        then:
        duration.durationInMillis == 5 * 60 * 1000
        duration.toMillis() == 5 * 60 * 1000
        duration.toSeconds() == 5 * 60
        duration.toMinutes() == 5

        duration.format('ss') == '300'
        duration.format('mm:ss') == '05:00'
        duration.toString() == '5m'

    }

    def 'test toString' () {

        expect:
        new Duration(100).toString() == '100ms'
        new Duration(1_000).toString() == '1s'
        new Duration(1_100).toString() == '1.1s'
        new Duration(32_300).toString() == '32.3s'
        new Duration(61 * 1000).toString() == '1m 1s'
        new Duration(61 * 1000 + 200).toString() == '1m 1s'
        new Duration(61 * 1000 + 800).toString() == '1m 2s'
        new Duration(60 * 60 * 1000 + 1000).toString() == '1h 1s'
        new Duration(25 * 60 * 60 * 1000 + 1000).toString() == '1d 1h 1s'

    }

    def 'test equals and compare' () {
        expect:
        new Duration('1h') == new Duration('1h')
        new Duration('10min') < new Duration('11min')
        new Duration('20min') > new Duration('11min')

    }

    def 'should add time' () {

        expect:
        new Duration('1 hour') + new Duration('3 hours')  == new Duration('4 hours')

    }

    def 'should subtract time' () {

        expect:
        new Duration('4 hour') - new Duration('3 hours')  == new Duration('1 hour')

    }

    def 'should multiple time' () {
        expect:
        new Duration('2 hour') * 3 == new Duration('6 hours')
        new Duration('6 hours') * 1.5 == new Duration('9 hours')
        // `multiply` a number by a MemoryUnit is implemented by `NumberDelegatingMetaClass`
    }

    def 'should divide time' () {
        expect:
        new Duration('6 hour') / 2 == new Duration('3 hours')
        new Duration('9 hours') / 1.5 == new Duration('6 hours')
    }


    def testThrottle() {

        when:
        int i = 0
        int result = -1
        5.times {
            result = Duration.of('100ms').throttle { (++i)*2 }
        }

        // it is invoked just one time
        // the result it is '2'
        then:
        i == 1
        result == 2

        when:
        i = 0
        result = -1
        5.times {
            result = Duration.of('100ms').throttle { (++i)*2 }
            sleep 30
        }

        // having a sleep of 20 millis and a delay of 100 millis, in 5 times it is called two times
        then:
        i == 2
        result == 4

    }

    def testThrottleWithSeed() {

        setup:

        when:
        int i = 0
        int result = -1
        5.times {
            result = Duration.of('100ms').throttle(33) { (++i)*2 }
        }
        then:
        // it is never invoked
        i == 0
        // and the 'seed' value it is returned
        result == 33


        when:
        i = 0
        result = -1
        5.times {
            result = Duration.of('100ms').throttle(null) { (++i)*2 }
            sleep 30
        }

        // it is never invoked ust one time
        then:
        i == 1
        result == 2

    }

}
