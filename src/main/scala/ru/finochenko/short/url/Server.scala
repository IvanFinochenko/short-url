package ru.finochenko.short.url

import cats.effect.{ContextShift, ExitCode}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}

import scala.concurrent.duration._

object Server extends TaskApp {

  override def run(args: List[String]): Task[ExitCode] = {

    implicit val contextShift: ContextShift[Task] = Task.contextShift
    implicit val logger: Logger[Task] = Slf4jLogger.getLogger[Task]

    sleepAndRetry(ServerBuilder[Task].build(), 5, 5.second)

  }

  private def sleepAndRetry[A](task: Task[A], maxRetries: Int, sleep: FiniteDuration)(implicit logger: Logger[Task]): Task[A] = {
    task.onErrorRecoverWith { e =>
      if (maxRetries > 0) {
        logger.error(e)(s"Error while start server. maxRetries = $maxRetries") *>
            Task.timer.sleep(sleep) *>
            sleepAndRetry(task, maxRetries - 1, sleep)
      } else {
        Task.raiseError(e)
      }
    }
  }

}
