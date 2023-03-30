import sbt.Keys._
import com.github.ingarabr._

val scala3Version = "3.2.1"
val zioVersion    = "2.0.5"

lazy val assemblySettings = Seq(
  assemblyMergeStrategy := {
    case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
    case "log4j.properties"                                  => MergeStrategy.discard
    case m if m.toLowerCase.startsWith("meta-inf/services/") =>
      MergeStrategy.filterDistinctLines
    case "reference.conf"                                    => MergeStrategy.concat
    case _                                                   => MergeStrategy.first
  }
)

lazy val root = project
  .in(file("."))
  .enablePlugins(CloudFunctionsPlugin)
  .settings(
    name                             := "gcfnsc",
    version                          := "0.1.0-SNAPSHOT",
    scalaVersion                     := scala3Version,
    assemblySettings,
    javacOptions ++= Seq("-source", "17", "-target", "17"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      "com.google.cloud"           % "google-cloud-spanner"    % "6.34.1",
      "dev.zio"                   %% "zio"                     % zioVersion,
      "dev.zio"                   %% "zio-streams"             % zioVersion,
      "dev.zio"                   %% "zio-test"                % zioVersion % Test,
      "dev.zio"                   %% "zio-test-sbt"            % zioVersion % Test,
      "dev.zio"                   %% "zio-http"                % "0.0.3",
      "com.google.cloud.functions" % "functions-framework-api" % "1.0.4"    % "provided",
      "org.scalameta"             %% "munit"                   % "0.7.29"   % Test
    ),
    cloudFunctionClass               := "ZIOHttpCloudFunctionSpanner",
    cloudFunctionJar                 := assembly.value,
    cloudFunctionDeployConfiguration := DeployConfiguration(
      functionName = "scala-zio-spanner-fn",
      gcpProject = "GCP_PROJECT",
      gcpLocation = "REGION",
      memoryMb = 512,
      triggerHttp = true,
      allowUnauthenticated = true,
      runtime = "java17",
      extraArgs = List.empty,
      releaseChannel = ReleaseChannel.GA
    )
  )
