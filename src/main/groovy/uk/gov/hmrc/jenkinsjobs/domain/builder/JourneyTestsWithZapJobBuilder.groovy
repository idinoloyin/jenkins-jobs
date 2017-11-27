package uk.gov.hmrc.jenkinsjobs.domain.builder

import uk.gov.hmrc.jenkinsjobbuilders.domain.publisher.Publisher
import uk.gov.hmrc.jenkinsjobbuilders.domain.scm.Scm
import uk.gov.hmrc.jenkinsjobbuilders.domain.step.Step
import uk.gov.hmrc.jenkinsjobbuilders.domain.step.SbtStep

import static uk.gov.hmrc.jenkinsjobbuilders.domain.configure.CucumberReportsPublisher.cucumberReportsPublisher
import static uk.gov.hmrc.jenkinsjobbuilders.domain.configure.XvfbBuildWrapper.xvfbBuildWrapper
import static uk.gov.hmrc.jenkinsjobbuilders.domain.publisher.ArtifactsPublisher.artifactsPublisher
import static uk.gov.hmrc.jenkinsjobbuilders.domain.step.ShellStep.shellStep
import static uk.gov.hmrc.jenkinsjobbuilders.domain.trigger.GitHubPushTrigger.gitHubPushTrigger
import static uk.gov.hmrc.jenkinsjobbuilders.domain.trigger.PollTrigger.pollTrigger

final class JourneyTestsWithZapJobBuilder extends AbstractJourneyTestsJobBuilder<JourneyTestsWithZapJobBuilder> {

    JourneyTestsWithZapJobBuilder(String name, String scm, SbtStep startStep, Step commandStep, SbtStep stopStep) {
        builder = uk.gov.hmrc.jenkinsjobs.domain.builder.JobBuilders.jobBuilder(name, scm, 'master').
                withTriggers(gitHubPushTrigger(), pollTrigger("H/5 * * * *")).
                withConfigures(xvfbBuildWrapper(), cucumberReportsPublisher()).
                withSteps(startStep, startZap(), commandStep, stopZap(), stopStep).
                withPublishers(uk.gov.hmrc.jenkinsjobs.domain.publisher.Publishers.defaultHtmlReportsPublisher(), uk.gov.hmrc.jenkinsjobs.domain.publisher.Publishers.cleanXvfbPostBuildTaskPublisher(), archiveArtifacts())
    }

    JourneyTestsWithZapJobBuilder(String name, String scm, SbtStep startStep, Step commandStep, SbtStep stopStep, Boolean withTriggers) {
        if (withTriggers) { JourneyTestsJobBuilder(name, scm, startStep, commandStep, stopStep) } else {
            builder = uk.gov.hmrc.jenkinsjobs.domain.builder.JobBuilders.jobBuilder(name, scm, 'master').
                    withConfigures(xvfbBuildWrapper(), cucumberReportsPublisher()).
                    withSteps(startStep, startZap(), commandStep, stopZap(), stopStep).
                    withPublishers(uk.gov.hmrc.jenkinsjobs.domain.publisher.Publishers.defaultHtmlReportsPublisher(), uk.gov.hmrc.jenkinsjobs.domain.publisher.Publishers.cleanXvfbPostBuildTaskPublisher(), archiveArtifacts())
        }
    }

    private static Step startZap() {
        shellStep("""\
                    |echo "Starting ZAP"
                    |mkdir -p results
                    |zap -daemon -config api.disablekey=true -port 11000 -dir \$WORKSPACE/results/zap -newsession zap-journey &
                    |sleep 10
                    """.stripMargin())
    }

    private static Step stopZap() {
        shellStep("""\
                  |echo "Killing Zap"
                  |curl --silent http://localhost:11000/HTML/core/action/shutdown
                  |sleep 10
                  """.stripMargin())
    }

    private static Publisher archiveArtifacts() {
        artifactsPublisher("results/zap/**")
    }
}
