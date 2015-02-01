package jetonmatik.provider

import java.time.Instant

trait NowProvider {
  def instant(): Instant
}

trait LocalNowProvider extends NowProvider {
  override def instant(): Instant = Instant.now()
}
