package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import groovy.util.CliBuilder
import org.springframework.core.env.Environment
import org.slf4j.*
import groovy.util.logging.Slf4j
import java.text.*

@Slf4j
@SpringBootApplication
class Application implements CommandLineRunner {

    //get commandline options like "--start" as env.getProperty('start')
    @Autowired
    Environment env

    @Autowired
    MultibeamRepository repository

    @Autowired
    MultibeamService service

    @Value('${lastrun.file}')
    lastRunFilename  //absolute path to file


    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Override
    void run(String... strings) throws Exception {
        Date startDate
        Date endDate
        List<String> surveys //one or more NGDC_ID values
        File cmds
        File outputDir


        def cli = new CliBuilder(usage: 'generate_mbtiles.groovy')
        cli.with {
            h longOpt: 'help', 'Show usage information'
            _ longOpt: 'survey', args: 1, argName: 'surveyId', 'generate tile(s) for given survey'
            _ longOpt: 'start', args: 1, argName: 'start date', 'limit to surveys after this date. Overrides lastrun file'
            _ longOpt: 'end', args: 1, argName: 'end date', 'limit to surveys before this date'
            _ longOpt: 'batch', args: 1, argName: 'filename', 'write commands to specified shell script'
            _ longOpt: 'dryrun', 'print out actions without actually executing commands'
            _ longOpt: 'cleanup', 'remove unneeded files or those without any valid data'
            _ longOpt: 'report', 'report on survey products'
            _ longOpt: 'output', args: 1, argName: 'filename', 'required': true, 'output directory to use. required'
        }
        def options = cli.parse(strings)

        //catches command line parse errors
        if (!options) {
            log.error "Error parsing command line arguments"
            return
        }

        //print out command line options
        if (options.h) {
            cli.usage()
            return
        }

        if (options.start) {
            log.info "Overriding start date in ${lastRunFilename}"
            try {
                //defaults to local time zone
                startDate = Date.parse('yyyy-MM-dd', options.start)
            } catch (e) {
                log.error "invalid format for start date"
                return
            }
        } else {
            File lastRunFile = new File(lastRunFilename)
            if (lastRunFile.exists()) {
                try {
                    //TODO clearTime() to force time to 00:00:00?
                    startDate = Date.parse('yyyy-MM-dd HH:mm:ss', lastRunFile.text)
                    log.info "Using start date from ${lastRunFilename}"
                } catch (e) {
                    log.error "invalid date format in ${lastRunFile}"
                }
            }
            log.info "start date is ${startDate}"
        }

        if (options.end) {
            //TODO only allow end date in conjunction w/ start date?
            try {
                //defaults to local time zone
                endDate = Date.parse('yyyy-MM-dd', options.end)
            } catch (e) {
                log.error "invalid format for end date"
                return
            }
        }

        //TODO expand to allow CSV list of surveys?
        if (options.survey) {
            log.debug "survey ${options.survey} requested, start and end date options don't apply"
            if (! repository.surveyExists(options.survey)) {
                log.error "requested survey ${options.survey} not found in database"
                return
            }
            surveys = [options.survey]
        }

        if (options.output) {
            outputDir = new File(options.output)
            if (outputDir.exists()) {
                log.info "using existing output directory ${outputDir}"
            } else {
                log.info "creating new output directory ${outputDir}"
                assert outputDir.mkdirs()
            }
        }

        //TODO should output option be required w/ batch
        if (options.batch) {
            cmds = new File(outputDir, options.batch)
            if (cmds.exists()) {
                log.info "removing exisiting batch file ${cmds}"
                cmds.delete()
            }
            cmds.write "# batch file generated ${new Date()}\n"
        }

        //////////////////////////////////////
        //DEBUG: reset surveys to force use of date range
        surveys = null
        //////////////////////////////////////

        //use date range to locate surveys to process
        if (! surveys) {
            surveys = repository.getSurveysByDate(startDate, endDate)
        }


        repository.getSurveyExtent('NEW2597')

        surveys.each { surveyId ->
            log.debug "processing survey ${surveyId}..."
            if (options.batch) {
                cmds << "## survey ${surveyId} ##\n"
            }

            service.cleanupFiles(outputDir, surveyId)

            List potentialTiles = service.getPotentialTiles(surveyId)

        }

    }




}
